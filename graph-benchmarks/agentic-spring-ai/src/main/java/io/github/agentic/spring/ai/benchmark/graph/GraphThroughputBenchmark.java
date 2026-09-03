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

/** 共享预编译十节点图在常见开发机线程数下的总吞吐。 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
@State(Scope.Benchmark)
public class GraphThroughputBenchmark {

	private CompiledGraph graph;
	private RunnableConfig config;

	@Setup(Level.Trial)
	public void setup() throws Exception {
		graph = GraphBenchmarkSupport.sequential(10);
		config = RunnableConfig.builder().build();
	}

	@Benchmark
	@Threads(1)
	public int threads1() {
		return invoke();
	}

	@Benchmark
	@Threads(4)
	public int threads4() {
		return invoke();
	}

	@Benchmark
	@Threads(8)
	public int threads8() {
		return invoke();
	}

	private int invoke() {
		return ((Number) graph.invoke(Map.of("value", 0), config)
			.orElseThrow().value("value").orElseThrow()).intValue();
	}
}
