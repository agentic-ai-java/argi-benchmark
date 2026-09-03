/*
 * Copyright 2026 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.agentic.spring.ai.benchmark.graph;

import io.github.agentic.spring.ai.graph.CompiledGraph;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/** 图构建和 compile 冷路径开销。 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@State(Scope.Thread)
public class CompileGraphBenchmark {

	@Param({"1", "10", "50"})
	public int nodeCount;

	@Benchmark
	public CompiledGraph compile() throws Exception {
		return GraphBenchmarkSupport.sequential(nodeCount);
	}
}
