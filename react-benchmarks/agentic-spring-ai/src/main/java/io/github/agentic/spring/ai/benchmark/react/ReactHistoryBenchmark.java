/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.agentic.spring.ai.benchmark.react;

import io.github.agentic.spring.ai.graph.RunnableConfig;
import io.github.agentic.spring.ai.graph.agent.ReactAgent;
import io.github.agentic.spring.ai.graph.checkpoint.savers.MemorySaver;
import org.openjdk.jmh.annotations.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 固定长度历史消息输入下的无网络 ReAct 编排开销。 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
public class ReactHistoryBenchmark {

	@State(Scope.Thread)
	public static class HistoryState {
		@Param({"0", "5", "25"})
		public int historyPairs;

		ReactAgent agent;
		RunnableConfig config;
		List<Message> messages;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			agent = ReactAgent.builder()
				.name("history_agent")
				.model(new AgenticSpringReactBenchmark.FixedChatModel())
				.saver(MemorySaver.builder().build())
				.releaseThread(true)
				.build();
			config = RunnableConfig.builder().threadId("jmh-history").build();
			List<Message> history = new ArrayList<>(historyPairs * 2 + 1);
			for (int i = 0; i < historyPairs; i++) {
				history.add(UserMessage.builder().text("question-" + i).build());
				history.add(new AssistantMessage("answer-" + i));
			}
			history.add(UserMessage.builder().text("current-question").build());
			messages = List.copyOf(history);
		}
	}

	@Benchmark
	public String call(HistoryState state) throws Exception {
		return state.agent.call(state.messages, state.config).getText();
	}
}
