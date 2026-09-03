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

/** 用可控 CPU 工作量寻找串行与并行调度的收益交叉点。 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
public class ParallelCrossoverGraphBenchmark {

	@State(Scope.Thread)
	public static class WorkState {
		@Param({"4", "8"})
		public int branches;

		@Param({"0", "10000", "100000"})
		public long cpuTokens;

		CompiledGraph sequential;
		CompiledGraph parallel;
		RunnableConfig sequentialConfig;
		RunnableConfig parallelConfig;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			sequential = GraphBenchmarkSupport.sequentialWorkload(branches, cpuTokens);
			parallel = GraphBenchmarkSupport.parallelFanOut(branches, cpuTokens);
			sequentialConfig = RunnableConfig.builder().build();
			parallelConfig = GraphBenchmarkSupport.parallelConfig();
		}
	}

	@Benchmark
	public int sequential(WorkState state) {
		return GraphBenchmarkSupport.value(state.sequential, state.sequentialConfig);
	}

	@Benchmark
	public int parallel(WorkState state) {
		return ((Number) state.parallel.invoke(Map.of(), state.parallelConfig)
			.orElseThrow().value("completed").orElseThrow()).intValue();
	}
}
