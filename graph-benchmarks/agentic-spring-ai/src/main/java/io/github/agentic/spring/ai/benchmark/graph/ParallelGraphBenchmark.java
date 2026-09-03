/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.agentic.spring.ai.benchmark.graph;

import io.github.agentic.spring.ai.graph.CompiledGraph;
import io.github.agentic.spring.ai.graph.RunnableConfig;
import org.openjdk.jmh.annotations.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 多分支 ALL_OF 扇出/汇合调度开销，不在节点内注入人为延迟。 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
public class ParallelGraphBenchmark {

	@State(Scope.Thread)
	public static class ParallelState {
		@Param({"2", "8", "32"})
		public int branches;

		CompiledGraph graph;
		RunnableConfig config;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			graph = GraphBenchmarkSupport.parallelFanOut(branches);
			config = GraphBenchmarkSupport.parallelConfig();
		}
	}

	@Benchmark
	public int invoke(ParallelState state) {
		return ((Number) state.graph.invoke(Map.of(), state.config)
			.orElseThrow().value("completed").orElseThrow()).intValue();
	}
}
