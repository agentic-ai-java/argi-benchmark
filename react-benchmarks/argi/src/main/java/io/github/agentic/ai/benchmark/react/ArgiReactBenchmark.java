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
package io.github.agentic.ai.benchmark.react;

import io.github.agentic.ai.graph.agent.ReactAgent;
import io.github.agentic.ai.graph.checkpoint.savers.MemorySaver;
import org.openjdk.jmh.annotations.*;
import io.github.agentic.ai.api.chat.ChatClient;
import io.github.agentic.ai.api.chat.ChatClientCapabilities;
import io.github.agentic.ai.api.chat.ChatEvent;
import io.github.agentic.ai.api.chat.ChatMessage;
import io.github.agentic.ai.api.chat.ChatRequest;
import io.github.agentic.ai.api.chat.ChatResponse;
import io.github.agentic.ai.api.chat.ChatRole;
import io.github.agentic.ai.api.chat.FinishReason;
import io.github.agentic.ai.api.chat.ToolCall;
import io.github.agentic.ai.api.chat.ToolDefinition;
import io.github.agentic.ai.api.tool.Tool;
import io.github.agentic.ai.api.tool.ToolContext;
import io.github.agentic.ai.api.tool.ToolRegistry;
import io.github.agentic.ai.api.tool.ToolResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Deterministic, in-process ReAct benchmarks measuring orchestration overhead,
 * rather than network latency or model inference.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
public class ArgiReactBenchmark {

	@State(Scope.Thread)
	public static class ReactState {
		FixedChatClient fixedModel;
		ToolRoundTripChatClient toolModel;
		ReactAgent noToolAgent;
		ReactAgent toolAgent;
		ChatRequest directPrompt;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			fixedModel = new FixedChatClient();
			toolModel = new ToolRoundTripChatClient();
			directPrompt = ChatRequest.of(List.of(ChatMessage.user("ping")));
			noToolAgent = ReactAgent.builder()
				.name("fixed_agent")
				.chatClient(fixedModel)
				.saver(MemorySaver.builder().build())
				.releaseThread(true)
				.build();
			toolAgent = ReactAgent.builder()
				.name("tool_agent")
				.chatClient(toolModel)
				.toolRegistry(new ToolRegistry(List.of(new LocalTool())))
				.saver(MemorySaver.builder().build())
				.releaseThread(true)
				.build();
		}
	}

	@Benchmark
	public String directModelBaseline(ReactState state) {
		return state.fixedModel.callBlocking(state.directPrompt).text();
	}

	@Benchmark
	public String oneModelTurn(ReactState state) throws Exception {
		return state.noToolAgent.call("ping").text();
	}

	@Benchmark
	public String oneToolRoundTrip(ReactState state) throws Exception {
		return state.toolAgent.call("lookup locally").text();
	}

	static class FixedChatClient implements ChatClient {
		@Override
		public CompletableFuture<ChatResponse> call(ChatRequest request) {
			return CompletableFuture.completedFuture(response(ChatMessage.assistant("fixed-response")));
		}

		@Override
		public Flow.Publisher<ChatEvent> stream(ChatRequest request) {
			throw new UnsupportedOperationException("This benchmark measures synchronous model turns");
		}

		@Override
		public ChatClientCapabilities capabilities() {
			return new ChatClientCapabilities(false, true, false, false, true, false);
		}
	}

	static final class ToolRoundTripChatClient extends FixedChatClient {
		@Override
		public CompletableFuture<ChatResponse> call(ChatRequest request) {
			boolean toolAlreadyExecuted = request.messages().stream()
				.anyMatch(message -> message.role() == ChatRole.TOOL);
			if (toolAlreadyExecuted) {
				return CompletableFuture.completedFuture(response(ChatMessage.assistant("tool-finished")));
			}
			ToolCall call = new ToolCall("call-1", "local_lookup", "{}");
			return CompletableFuture.completedFuture(response(ChatMessage.assistant("", List.of(call))));
		}
	}

	static final class LocalTool implements Tool {
		@Override
		public ToolDefinition definition() {
			return new ToolDefinition("local_lookup", "Returns a fixed local result", "{\"type\":\"object\"}");
		}

		@Override
		public CompletableFuture<ToolResult> execute(ToolCall call, ToolContext context) {
			return CompletableFuture.completedFuture(ToolResult.text("local-result"));
		}
	}

	private static ChatResponse response(ChatMessage message) {
		return new ChatResponse(null, "benchmark", List.of(message), message.toolCalls(),
			message.toolCalls().isEmpty() ? FinishReason.STOP : FinishReason.TOOL_CALLS, null, Map.of());
	}
}
