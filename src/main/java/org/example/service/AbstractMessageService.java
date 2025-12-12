package org.example.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.dingtalkrobot_1_0.Client;
import com.aliyun.dingtalkrobot_1_0.models.BatchSendOTOHeaders;
import com.aliyun.dingtalkrobot_1_0.models.BatchSendOTORequest;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.cache.ChatHistory;
import org.example.entity.SendMessageType;
import org.example.utils.ChatbotMessageUtils;
import org.example.utils.DateUtils;
import org.example.utils.MessageUtils;
import org.example.utils.StrUtils;
import org.springframework.ai.openai.api.OpenAiApi;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author ：sunziyou
 * @date ：Created in 2025/12/13 19:47
 * @modified By：
 */
public class AbstractMessageService {
    private static Logger logger = LogManager.getLogger(AbstractMessageService.class);
    private static final String ENCRYPTION_ALGORITHM = "HmacSHA256";
    private static final String CHARSET = "utf-8";
    public Client initClient() throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.protocol = "https";
        config.regionId = "central";
        return  new Client(config);
    }
    protected List<OpenAiApi.ChatCompletionMessage>  getMessages(ChatbotMessage chatbotMessage,String fileName){
        String senderStaffId = chatbotMessage.getSenderStaffId();
        List<OpenAiApi.ChatCompletionMessage> messages = ChatHistory.get(senderStaffId);
        if (messages.size() == 0) {
            messages.add(createSystem(chatbotMessage,fileName));
        } else {
            messages.add(CreateUser(chatbotMessage));
        }
        return messages;
    }

    /**
     * 使用HmacSHA256算法计算签名
     *
     * @param currentTimeMillis
     * @param secret
     * @return
     */
    protected String assembleSign(long currentTimeMillis, String secret) {
        String sign = "";
        try {
            String stringToSign = currentTimeMillis + String.valueOf(StrUtil.C_LF) + secret;
            Mac mac = Mac.getInstance(ENCRYPTION_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(CHARSET), ENCRYPTION_ALGORITHM));
            byte[] signData = mac.doFinal(stringToSign.getBytes(CHARSET));
            sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)), CHARSET);
            return sign;
        } catch (Exception e) {
            logger.warn("DingDingHandler#assembleSign fail!:{}", e);
        }
        throw new RuntimeException("生成钉钉机器人签名信息错误");
    }
    protected DingDingRobotParam assembleParam(String content,ChatbotMessage chatbotMessage){
        // 接收者相关
        DingDingRobotParam.AtVO atVo = DingDingRobotParam.AtVO.builder().build();
        Set<String> receives = new HashSet<>();
        receives.add(chatbotMessage.getSenderStaffId());
        atVo.setAtUserIds(new ArrayList<>(receives));
        DingDingRobotParam param = DingDingRobotParam.builder().at(atVo)
                .msgtype(SendMessageType.TEXT.getDingDingRobotType()).build();
        param.setText(DingDingRobotParam.TextVO.builder().content(content).build());
        return param;
    }
    protected void sendContentToRobotCode(String accessToken, String content, Client robotClient,String robotCode,String staffId) {
        BatchSendOTOHeaders batchSendOTOHeaders = new BatchSendOTOHeaders();

        batchSendOTOHeaders.xAcsDingtalkAccessToken = accessToken;
        JSONObject msgParam = new JSONObject();
        msgParam.put("text", content.replaceAll("\n", "  \n"));
        BatchSendOTORequest batchSendOTORequest = new BatchSendOTORequest()
                .setRobotCode(robotCode)
                .setUserIds(Arrays.asList(
                        staffId
                ))
                .setMsgKey("sampleMarkdown")
                .setMsgParam(msgParam.toString());
        try {
            robotClient.batchSendOTOWithOptions(batchSendOTORequest, batchSendOTOHeaders, new RuntimeOptions());
        } catch (TeaException err) {
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                logger.warn("RobotGroupMessagesService_sendPrivateMessage err.code={}, err.message={}", err.code, err.message);
            }

        } catch (Exception _err) {
            TeaException err = new TeaException(_err.getMessage(), _err);
            if (!com.aliyun.teautil.Common.empty(err.code) && !com.aliyun.teautil.Common.empty(err.message)) {
                logger.warn("RobotGroupMessagesService_sendPrivateMessage err.code={}, err.message={}", err.code, err.message);
            }

        }
    }

    protected void  addAssistToHistory(List<OpenAiApi.ChatCompletionMessage> messages,ChatbotMessage chatbotMessage,String jsonString){
        OpenAiApi.ChatCompletionMessage assistantMessage = MessageUtils.createAssistantMessage(jsonString);
        messages.add(assistantMessage);
        String senderStaffId=chatbotMessage.getSenderStaffId();
        ChatHistory.put(senderStaffId, messages);
    }
    private OpenAiApi.ChatCompletionMessage createSystem(ChatbotMessage chatbotMessage,String fileName) {
        String message = StrUtils.readByResource(fileName);
        Map<String, String> varValue = new HashMap<>();
        varValue.put("input", ChatbotMessageUtils.getUserContent(chatbotMessage));
        varValue.put("dateTime", DateUtils.getCurrentTime());
        String systemMessage = MessageUtils.createMessage(varValue, message);
        return MessageUtils.createSystemMessage(systemMessage);
    }
    private OpenAiApi.ChatCompletionMessage CreateUser(ChatbotMessage chatbotMessage) {
        return MessageUtils.createUserMessage(ChatbotMessageUtils.getUserContent(chatbotMessage));
    }
}
