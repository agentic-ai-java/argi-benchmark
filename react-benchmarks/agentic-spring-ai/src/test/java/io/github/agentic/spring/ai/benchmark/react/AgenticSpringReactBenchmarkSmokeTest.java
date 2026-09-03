/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.agentic.spring.ai.benchmark.react;

import io.github.agentic.spring.ai.graph.RunnableConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgenticSpringReactBenchmarkSmokeTest {

	@Test
	void deterministicModelAndToolPathsComplete() throws Exception {
		AgenticSpringReactBenchmark benchmark = new AgenticSpringReactBenchmark();
		AgenticSpringReactBenchmark.ReactState state = new AgenticSpringReactBenchmark.ReactState();
		state.setup();
		assertEquals("fixed-response", benchmark.oneModelTurn(state));
		assertEquals("tool-finished", benchmark.oneToolRoundTrip(state));
	}

	@Test
	void historyAndSharedThroughputPathsComplete() throws Exception {
		ReactHistoryBenchmark historyBenchmark = new ReactHistoryBenchmark();
		ReactHistoryBenchmark.HistoryState historyState = new ReactHistoryBenchmark.HistoryState();
		historyState.historyPairs = 5;
		historyState.setup();
		assertEquals("fixed-response", historyBenchmark.call(historyState));

		ReactThroughputBenchmark throughputBenchmark = new ReactThroughputBenchmark();
		ReactThroughputBenchmark.SharedState shared = new ReactThroughputBenchmark.SharedState();
		shared.setup();
		ReactThroughputBenchmark.ThreadState thread = new ReactThroughputBenchmark.ThreadState();
		thread.config = RunnableConfig.builder().threadId("smoke-thread").build();
		assertEquals("fixed-response", throughputBenchmark.threads1(shared, thread));
	}
}
