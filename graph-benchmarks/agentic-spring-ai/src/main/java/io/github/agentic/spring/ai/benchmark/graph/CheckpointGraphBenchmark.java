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

/** 单节点图启用 MemorySaver 后的调用成本；每次执行结束释放线程状态。 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
@State(Scope.Thread)
public class CheckpointGraphBenchmark {

	private CompiledGraph graph;
	private RunnableConfig config;

	@Setup(Level.Trial)
	public void setup() throws Exception {
		graph = GraphBenchmarkSupport.withMemoryCheckpoint();
		config = GraphBenchmarkSupport.checkpointConfig();
	}

	@Benchmark
	public int invokeWithMemorySaver() {
		return ((Number) graph.invoke(Map.of("value", 0), config)
			.orElseThrow().value("value").orElseThrow()).intValue();
	}
}
