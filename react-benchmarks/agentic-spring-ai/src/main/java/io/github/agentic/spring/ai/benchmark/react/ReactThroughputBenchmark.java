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
import org.openjdk.jmh.infra.ThreadParams;

import java.util.concurrent.TimeUnit;

/** 多线程共享同一预编译 ReactAgent 时的总吞吐。 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
public class ReactThroughputBenchmark {

	@State(Scope.Benchmark)
	public static class SharedState {
		ReactAgent agent;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			agent = ReactAgent.builder()
				.name("throughput_agent")
				.model(new AgenticSpringReactBenchmark.FixedChatModel())
				.saver(MemorySaver.builder().build())
				.releaseThread(true)
				.build();
			agent.call("compile-warmup", RunnableConfig.builder().threadId("setup").build());
		}
	}

	@State(Scope.Thread)
	public static class ThreadState {
		RunnableConfig config;

		@Setup(Level.Trial)
		public void setup(ThreadParams params) {
			config = RunnableConfig.builder().threadId("jmh-thread-" + params.getThreadIndex()).build();
		}
	}

	@Benchmark
	@Threads(1)
	public String threads1(SharedState shared, ThreadState thread) throws Exception {
		return invoke(shared, thread);
	}

	@Benchmark
	@Threads(4)
	public String threads4(SharedState shared, ThreadState thread) throws Exception {
		return invoke(shared, thread);
	}

	@Benchmark
	@Threads(8)
	public String threads8(SharedState shared, ThreadState thread) throws Exception {
		return invoke(shared, thread);
	}

	private String invoke(SharedState shared, ThreadState thread) throws Exception {
		return shared.agent.call("ping", thread.config).getText();
	}
}
