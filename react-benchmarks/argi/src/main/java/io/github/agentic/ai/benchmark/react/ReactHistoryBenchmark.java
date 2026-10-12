/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.agentic.ai.benchmark.react;

import io.github.agentic.ai.graph.RunnableConfig;
import io.github.agentic.ai.graph.agent.ReactAgent;
import io.github.agentic.ai.graph.checkpoint.savers.MemorySaver;
import org.openjdk.jmh.annotations.*;
import io.github.agentic.ai.api.chat.ChatMessage;

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
		List<ChatMessage> messages;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			agent = ReactAgent.builder()
				.name("history_agent")
				.chatClient(new ArgiReactBenchmark.FixedChatClient())
				.saver(MemorySaver.builder().build())
				.releaseThread(true)
				.build();
			config = RunnableConfig.builder().threadId("jmh-history").build();
			List<ChatMessage> history = new ArrayList<>(historyPairs * 2 + 1);
			for (int i = 0; i < historyPairs; i++) {
				history.add(ChatMessage.user("question-" + i));
				history.add(ChatMessage.assistant("answer-" + i));
			}
			history.add(ChatMessage.user("current-question"));
			messages = List.copyOf(history);
		}
	}

	@Benchmark
	public String call(HistoryState state) throws Exception {
		return state.agent.call(state.messages, state.config).text();
	}
}
