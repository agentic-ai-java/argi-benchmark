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
package io.github.agentic.spring.ai.benchmark.react;

import io.github.agentic.spring.ai.graph.agent.ReactAgent;
import io.github.agentic.spring.ai.graph.checkpoint.savers.MemorySaver;
import org.openjdk.jmh.annotations.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Agentic Spring AI ReAct 基准。
 *
 * <p>模型与工具均在进程内确定性执行，用于测量框架编排开销，而非网络或模型推理速度。</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 8, time = 1)
@Fork(3)
public class AgenticSpringReactBenchmark {

	@State(Scope.Thread)
	public static class ReactState {
		FixedChatModel fixedModel;
		ToolRoundTripChatModel toolModel;
		ReactAgent noToolAgent;
		ReactAgent toolAgent;
		Prompt directPrompt;

		@Setup(Level.Trial)
		public void setup() throws Exception {
			fixedModel = new FixedChatModel();
			toolModel = new ToolRoundTripChatModel();
			directPrompt = new Prompt("ping");
			noToolAgent = ReactAgent.builder()
				.name("fixed_agent")
				.model(fixedModel)
				.saver(MemorySaver.builder().build())
				.releaseThread(true)
				.build();
			toolAgent = ReactAgent.builder()
				.name("tool_agent")
				.model(toolModel)
				.methodTools(new LocalTools())
				.saver(MemorySaver.builder().build())
				.releaseThread(true)
				.build();
		}
	}

	@Benchmark
	public String directModelBaseline(ReactState state) {
		return state.fixedModel.call(state.directPrompt).getResult().getOutput().getText();
	}

	@Benchmark
	public String oneModelTurn(ReactState state) throws Exception {
		return state.noToolAgent.call("ping").getText();
	}

	@Benchmark
	public String oneToolRoundTrip(ReactState state) throws Exception {
		return state.toolAgent.call("lookup locally").getText();
	}

	static final class FixedChatModel implements ChatModel {
		@Override
		public ChatResponse call(Prompt prompt) {
			return response(new AssistantMessage("fixed-response"));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}
	}

	static final class ToolRoundTripChatModel implements ChatModel {
		@Override
		public ChatResponse call(Prompt prompt) {
			boolean toolAlreadyExecuted = prompt.getInstructions().stream()
				.anyMatch(ToolResponseMessage.class::isInstance);
			if (toolAlreadyExecuted) {
				return response(new AssistantMessage("tool-finished"));
			}
			AssistantMessage.ToolCall call = new AssistantMessage.ToolCall(
				"call-1", "function", "local_lookup", "{}");
			return response(AssistantMessage.builder().content("").toolCalls(List.of(call)).build());
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}
	}

	static final class LocalTools {
		@Tool(name = "local_lookup", description = "返回固定的本地查询结果")
		public String lookup() {
			return "local-result";
		}
	}

	private static ChatResponse response(AssistantMessage message) {
		return new ChatResponse(List.of(new Generation(message)));
	}
}
