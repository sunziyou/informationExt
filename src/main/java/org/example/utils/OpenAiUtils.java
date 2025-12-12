package org.example.utils;

import org.example.parser.ParserToolFactory;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

public class OpenAiUtils {
    public static String  invokeLLm(OpenAiApi openAiApi, List<OpenAiApi.ChatCompletionMessage> messages,String moduleName){
        OpenAiApi.ChatCompletionRequest request = new OpenAiApi.ChatCompletionRequest(messages, moduleName, 0.0d, true);
        Flux<OpenAiApi.ChatCompletionChunk> chatCompletionChunkFlux = openAiApi.chatCompletionStream(request);
        String fullContent = chatCompletionChunkFlux
                .flatMap(chunk -> {
                    if (chunk.choices() == null || chunk.choices().isEmpty()) {
                        return Mono.empty();
                    }
                    String content = chunk.choices().get(0).delta().content();
                    return Mono.justOrEmpty(content);
                })
                .reduce("", (a, b) -> a + b)
                .onErrorResume(e -> {
                    return Mono.just(e.getMessage()); // 返回默认值
                }).block(Duration.ofSeconds(240));
        return  ParserToolFactory.createParserTool(moduleName).parseJson(fullContent);
    }
}
