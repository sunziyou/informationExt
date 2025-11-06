package org.example.utils;

import org.springframework.ai.openai.api.OpenAiApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageUtils {
    private static final String begain = "%{";
    private static final String end = "}%";

    public static List<OpenAiApi.ChatCompletionMessage> createMessage(String system, String user) {
        List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>();
        String systemall=system+"\n用户输入:"+user;
        messages.add(new OpenAiApi.ChatCompletionMessage(systemall, OpenAiApi.ChatCompletionMessage.Role.SYSTEM));
        //messages.add(new OpenAiApi.ChatCompletionMessage(user, OpenAiApi.ChatCompletionMessage.Role.USER));
        return messages;
    }

    public static OpenAiApi.ChatCompletionMessage createUserMessage(String user) {
        return new OpenAiApi.ChatCompletionMessage(user, OpenAiApi.ChatCompletionMessage.Role.USER);
    }
    public static OpenAiApi.ChatCompletionMessage createAssistantMessage(String user) {
        return new OpenAiApi.ChatCompletionMessage(user, OpenAiApi.ChatCompletionMessage.Role.ASSISTANT);
    }
    public static OpenAiApi.ChatCompletionMessage createSystemMessage(String system) {
        return new OpenAiApi.ChatCompletionMessage(system, OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
    }

    public static OpenAiApi.ChatCompletionMessage createToolMessage(String tool) {
        return new OpenAiApi.ChatCompletionMessage(tool, OpenAiApi.ChatCompletionMessage.Role.USER);
    }

    public static String createMessage(Map<String, String> varValue, String message) {
        for (var key : varValue.keySet()) {
            String value = varValue.get(key);
            String keyValue = begain + key + end;
            if (message.contains(keyValue)) {
                message = message.replace(keyValue, value);
            }
        }
        return message;
    }
}
