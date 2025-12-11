package org.example.entity.botMessage;

import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import lombok.Getter;

public class BoxChatbotMessage {
    @Getter
    private int errorCount = 0;
    @Getter
    private ChatbotMessage chatbotMessage;

    public BoxChatbotMessage(ChatbotMessage chatbotMessage) {
        this.chatbotMessage = chatbotMessage;
    }

    public void error() {
        errorCount++;
    }

}
