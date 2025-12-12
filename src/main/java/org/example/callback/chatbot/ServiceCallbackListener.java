package org.example.callback.chatbot;

import com.alibaba.fastjson.JSONObject;
import com.dingtalk.open.app.api.callback.OpenDingTalkCallbackListener;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import lombok.extern.slf4j.Slf4j;
import org.example.service.SaleGroupMessagesService;
import org.example.service.ServiceGroupMessagesService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 机器人消息回调
 *
 * @author zeymo
 */
@Slf4j
@Component
public class ServiceCallbackListener implements OpenDingTalkCallbackListener<ChatbotMessage, JSONObject>, InitializingBean {
    private BlockingQueue<ChatbotMessage> messageQueue = new ArrayBlockingQueue<>(1000);
    private ServiceGroupMessagesService serviceGroupMessagesService;

    @Autowired
    public ServiceCallbackListener(ServiceGroupMessagesService serviceGroupMessagesService) {
        this.serviceGroupMessagesService = serviceGroupMessagesService;
    }

    /**
     * https://open.dingtalk.com/document/orgapp/the-application-robot-in-the-enterprise-sends-group-chat-messages
     *
     * @param message
     * @return
     */
    @Override
    public JSONObject execute(ChatbotMessage message) {
        try {
            messageQueue.put(message);
        } catch (Exception e) {
            log.error("receive group message by robot error:" + e.getMessage(), e);
        }
        return new JSONObject();
    }

    @Override
    public void afterPropertiesSet() {
        new Thread(() -> {
            while (true) {
                try {
                    ChatbotMessage chatbotMessage = messageQueue.take();
                    executeInfo(chatbotMessage);
                } catch (Exception e) {
                    log.error("receive group message by robot error:" + e.getMessage(), e);
                }
            }
        }).start();
    }

    private void executeInfo(ChatbotMessage chatbotMessage) {
        try {
            serviceGroupMessagesService.sendPrivateMessage(chatbotMessage);
        }catch (Exception e){
            log.warn("执行业务逻辑错误",e);
        }
    }
}
