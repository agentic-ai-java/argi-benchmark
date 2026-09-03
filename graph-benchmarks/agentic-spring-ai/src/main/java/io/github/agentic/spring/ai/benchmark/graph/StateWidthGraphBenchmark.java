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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 日常业务状态逐渐变宽时，直线图的调用与分配量变化。 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
public class StateWidthGraphBenchmark {

	@State(Scope.Thread)
	public static class GraphState {
		@Param({"5", "20"})
		public int nodeCount;

		@Param({"1", "10", "50"})
		public int stateKeys;

		CompiledGraph graph;
		RunnableConfig config;
		Map<String, Object> input;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			graph = GraphBenchmarkSupport.sequentialWithStateWidth(nodeCount, stateKeys);
			config = RunnableConfig.builder().build();
			input = GraphBenchmarkSupport.inputWithStateWidth(stateKeys);
		}
	}

	@Benchmark
	public int directMapBaseline(GraphState state) {
		Map<String, Object> data = new HashMap<>(state.input);
		for (int i = 0; i < state.nodeCount; i++) {
			data.put("value", ((Number) data.get("value")).intValue() + 1);
		}
		return ((Number) data.get("value")).intValue();
	}

	@Benchmark
	public int invoke(GraphState state) {
		return ((Number) state.graph.invoke(new HashMap<>(state.input), state.config)
			.orElseThrow().value("value").orElseThrow()).intValue();
	}
}
