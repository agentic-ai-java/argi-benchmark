/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.agentic.spring.ai.benchmark.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GraphBenchmarkSmokeTest {

	@Test
	void sequentialAndLoopProduceExpectedState() throws Exception {
		assertEquals(10, GraphBenchmarkSupport.value(GraphBenchmarkSupport.sequential(10),
			io.github.agentic.spring.ai.graph.RunnableConfig.builder().build()));
		assertEquals(100, GraphBenchmarkSupport.value(GraphBenchmarkSupport.conditionalLoop(100),
			io.github.agentic.spring.ai.graph.RunnableConfig.builder().build()));
	}

	@Test
	void parallelFanOutCompletesAllBranches() throws Exception {
		int completed = ((Number) GraphBenchmarkSupport.parallelFanOut(8)
			.invoke(java.util.Map.of(), GraphBenchmarkSupport.parallelConfig())
			.orElseThrow().value("completed").orElseThrow()).intValue();
		assertEquals(8, completed);
	}

	@Test
	void developmentReferenceGraphsProduceExpectedState() throws Exception {
		assertEquals(20, GraphBenchmarkSupport.value(
			GraphBenchmarkSupport.sequentialWithStateWidth(20, 50),
			io.github.agentic.spring.ai.graph.RunnableConfig.builder().build()));
		assertEquals(4, GraphBenchmarkSupport.value(
			GraphBenchmarkSupport.sequentialWorkload(4, 100),
			io.github.agentic.spring.ai.graph.RunnableConfig.builder().build()));
		int completed = ((Number) GraphBenchmarkSupport.parallelFanOut(4, 100)
			.invoke(java.util.Map.of(), GraphBenchmarkSupport.parallelConfig())
			.orElseThrow().value("completed").orElseThrow()).intValue();
		assertEquals(4, completed);
	}
}
