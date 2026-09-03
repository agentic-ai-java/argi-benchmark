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

/** 条件边自循环，用于观察迭代增长曲线和深循环退化。 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
public class ConditionalLoopGraphBenchmark {

	@State(Scope.Thread)
	public static class LoopState {
		@Param({"10", "100", "1000"})
		public int iterations;

		CompiledGraph graph;
		RunnableConfig config;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			graph = GraphBenchmarkSupport.conditionalLoop(iterations);
			config = RunnableConfig.builder().build();
		}
	}

	@Benchmark
	public int invoke(LoopState state) {
		return ((Number) state.graph.invoke(Map.of("value", 0), state.config)
			.orElseThrow().value("value").orElseThrow()).intValue();
	}
}
