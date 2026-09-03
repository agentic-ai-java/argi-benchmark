/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.agentic.spring.ai.benchmark.graph;

import io.github.agentic.spring.ai.graph.CompileConfig;
import io.github.agentic.spring.ai.graph.CompiledGraph;
import io.github.agentic.spring.ai.graph.KeyStrategy;
import io.github.agentic.spring.ai.graph.RunnableConfig;
import io.github.agentic.spring.ai.graph.StateGraph;
import io.github.agentic.spring.ai.graph.checkpoint.config.SaverConfig;
import io.github.agentic.spring.ai.graph.checkpoint.savers.MemorySaver;
import io.github.agentic.spring.ai.graph.state.strategy.ReplaceStrategy;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import org.openjdk.jmh.infra.Blackhole;

import static io.github.agentic.spring.ai.graph.StateGraph.END;
import static io.github.agentic.spring.ai.graph.StateGraph.START;
import static io.github.agentic.spring.ai.graph.action.AsyncEdgeAction.edge_async;
import static io.github.agentic.spring.ai.graph.action.AsyncNodeAction.node_async;

/**
 * 构造语义固定的图，所有节点只做常量级状态更新，从而隔离 Graph 调度自身开销。
 */
final class GraphBenchmarkSupport {

	private GraphBenchmarkSupport() {
	}

	static CompileConfig noCheckpointConfig(int recursionLimit) {
		return CompileConfig.builder()
			.saverConfig(SaverConfig.builder().build())
			.recursionLimit(recursionLimit)
			.build();
	}

	static CompiledGraph sequential(int nodeCount) throws Exception {
		StateGraph graph = new StateGraph(GraphBenchmarkSupport::valueStrategy);
		for (int i = 0; i < nodeCount; i++) {
			String node = "node_" + i;
			graph.addNode(node, node_async(state -> Map.of("value", ((Number) state.value("value").orElse(0)).intValue() + 1)));
			graph.addEdge(i == 0 ? START : "node_" + (i - 1), node);
		}
		graph.addEdge("node_" + (nodeCount - 1), END);
		return graph.compile(noCheckpointConfig(nodeCount + 16));
	}

	static CompiledGraph conditionalLoop(int iterations) throws Exception {
		StateGraph graph = new StateGraph(GraphBenchmarkSupport::valueStrategy)
			.addNode("worker", node_async(state -> Map.of("value", ((Number) state.value("value").orElse(0)).intValue() + 1)))
			.addEdge(START, "worker")
			.addConditionalEdges("worker",
				edge_async(state -> ((Number) state.value("value").orElse(0)).intValue() < iterations ? "again" : "done"),
				Map.of("again", "worker", "done", END));
		return graph.compile(noCheckpointConfig(iterations + 16));
	}

	static CompiledGraph parallelFanOut(int branches) throws Exception {
		return parallelFanOut(branches, 0);
	}

	static CompiledGraph parallelFanOut(int branches, long cpuTokens) throws Exception {
		StateGraph graph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			for (int i = 0; i < branches; i++) {
				strategies.put("branch_" + i, new ReplaceStrategy());
			}
			strategies.put("completed", new ReplaceStrategy());
			return strategies;
		});
		for (int i = 0; i < branches; i++) {
			String branch = "branch_" + i;
			graph.addNode(branch, node_async(state -> {
				Blackhole.consumeCPU(cpuTokens);
				return Map.of(branch, 1);
			}))
				.addEdge(START, branch)
				.addEdge(branch, "merge");
		}
		graph.addNode("merge", node_async(state -> Map.of("completed", branches)))
			.addEdge("merge", END);
		return graph.compile(noCheckpointConfig(branches + 16));
	}

	static CompiledGraph sequentialWorkload(int nodes, long cpuTokens) throws Exception {
		StateGraph graph = new StateGraph(GraphBenchmarkSupport::valueStrategy);
		for (int i = 0; i < nodes; i++) {
			String node = "work_" + i;
			graph.addNode(node, node_async(state -> {
				Blackhole.consumeCPU(cpuTokens);
				return Map.of("value", ((Number) state.value("value").orElse(0)).intValue() + 1);
			}));
			graph.addEdge(i == 0 ? START : "work_" + (i - 1), node);
		}
		graph.addEdge("work_" + (nodes - 1), END);
		return graph.compile(noCheckpointConfig(nodes + 16));
	}

	static CompiledGraph sequentialWithStateWidth(int nodeCount, int stateKeys) throws Exception {
		StateGraph graph = new StateGraph(() -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("value", new ReplaceStrategy());
			for (int i = 0; i < stateKeys; i++) {
				strategies.put("state_" + i, new ReplaceStrategy());
			}
			return strategies;
		});
		for (int i = 0; i < nodeCount; i++) {
			String node = "wide_" + i;
			graph.addNode(node, node_async(state ->
				Map.of("value", ((Number) state.value("value").orElse(0)).intValue() + 1)));
			graph.addEdge(i == 0 ? START : "wide_" + (i - 1), node);
		}
		graph.addEdge("wide_" + (nodeCount - 1), END);
		return graph.compile(noCheckpointConfig(nodeCount + 16));
	}

	static Map<String, Object> inputWithStateWidth(int stateKeys) {
		Map<String, Object> input = new HashMap<>();
		input.put("value", 0);
		for (int i = 0; i < stateKeys; i++) {
			input.put("state_" + i, i);
		}
		return input;
	}

	static CompiledGraph withMemoryCheckpoint() throws Exception {
		CompileConfig config = CompileConfig.builder()
			.saverConfig(SaverConfig.builder().register(MemorySaver.builder().build()).build())
			.releaseThread(true)
			.build();
		return new StateGraph(GraphBenchmarkSupport::valueStrategy)
			.addNode("worker", node_async(state -> Map.of("value", ((Number) state.value("value").orElse(0)).intValue() + 1)))
			.addEdge(START, "worker")
			.addEdge("worker", END)
			.compile(config);
	}

	static RunnableConfig parallelConfig() {
		return RunnableConfig.builder().addParallelNodeExecutor(START, ForkJoinPool.commonPool()).build();
	}

	static RunnableConfig checkpointConfig() {
		return RunnableConfig.builder().threadId("jmh-memory-checkpoint").build();
	}

	static int value(CompiledGraph graph, RunnableConfig config) {
		return ((Number) graph.invoke(Map.of("value", 0), config)
			.orElseThrow()
			.value("value")
			.orElseThrow()).intValue();
	}

	private static Map<String, KeyStrategy> valueStrategy() {
		return Map.of("value", new ReplaceStrategy());
	}
}
