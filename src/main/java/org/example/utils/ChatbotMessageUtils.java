package org.example.utils;

import com.dingtalk.open.app.api.models.bot.ChatbotMessage;

public class ChatbotMessageUtils {
    public static String getUserContent(ChatbotMessage chatbotMessage){
        if(chatbotMessage.getText()==null){
            return chatbotMessage.getContent().getRecognition();
        }
        return chatbotMessage.getText().getContent();

    }
}
