/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.agentic.spring.ai.benchmark.graph;

import io.github.agentic.spring.ai.graph.CompiledGraph;
import io.github.agentic.spring.ai.graph.RunnableConfig;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/** 预编译直线图的端到端调用开销。 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
public class SequentialGraphBenchmark {

	@State(Scope.Thread)
	public static class GraphState {
		@Param({"1", "10", "50"})
		public int nodeCount;

		CompiledGraph graph;
		RunnableConfig config;
		List<Function<Map<String, Object>, Map<String, Object>>> directActions;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			graph = GraphBenchmarkSupport.sequential(nodeCount);
			config = RunnableConfig.builder().build();
			directActions = new ArrayList<>(nodeCount);
			for (int i = 0; i < nodeCount; i++) {
				directActions.add(input -> Map.of("value", ((Number) input.get("value")).intValue() + 1));
			}
		}
	}

	@Benchmark
	public int directJavaBaseline(GraphState state) {
		Map<String, Object> data = Map.of("value", 0);
		for (Function<Map<String, Object>, Map<String, Object>> action : state.directActions) {
			data = action.apply(data);
		}
		return ((Number) data.get("value")).intValue();
	}

	@Benchmark
	public int invoke(GraphState state) {
		return ((Number) state.graph.invoke(Map.of("value", 0), state.config)
			.orElseThrow()
			.value("value")
			.orElseThrow()).intValue();
	}
}
