package org.example.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.dingtalkrobot_1_0.Client;
import com.aliyun.dingtalkrobot_1_0.models.*;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import com.taobao.api.ApiException;
import com.taobao.api.internal.util.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.cache.ChatHistory;
import org.example.entity.DingDingRobotAccount;
import org.example.entity.Discussion;
import org.example.entity.SendMessageType;
import org.example.parser.ParserToolFactory;
import org.example.utils.DateUtils;
import org.example.utils.MessageUtils;
import org.example.utils.StrUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.*;

/**
 * @author zeymo
 */
@Slf4j
@Service
public class RobotGroupMessagesService {
    private static final String ENCRYPTION_ALGORITHM = "HmacSHA256";
    private static final String CHARSET = "utf-8";

    private static Logger logger = LogManager.getLogger(RobotGroupMessagesService.class);
    private static final String SEND_ALL = "@all";
    private Client robotClient;
    private final AccessTokenService accessTokenService;
    private static final String SEND_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2";

    @Value("${robot.code}")
    private String robotCode;

    @Value("${module}")
    private String moduleName;
    @Autowired
    private DingDingRobotAccount dingDingRobotAccount;

    @Autowired
    private DingDingWorkNoticeAccount dingDingWorkNoticeAccount;
   @Autowired
    private OpenAiApi openAiApi;

    @Autowired
    public RobotGroupMessagesService(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    @PostConstruct
    public void init() throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config();
        config.protocol = "https";
        config.regionId = "central";
        robotClient = new Client(config);
    }

    /**
     * send message to group with openConversationId
     *
     * @param openConversationId conversationId
     * @return messageId
     * @throws Exception e
     */
    public String send(String openConversationId, String text, String sendId) throws Exception {
        OrgGroupSendHeaders orgGroupSendHeaders = new OrgGroupSendHeaders();
        orgGroupSendHeaders.setXAcsDingtalkAccessToken(accessTokenService.getAccessToken());

        OrgGroupSendRequest orgGroupSendRequest = new OrgGroupSendRequest();
        orgGroupSendRequest.setMsgKey("sampleText");
        orgGroupSendRequest.setRobotCode(robotCode);

        orgGroupSendRequest.setOpenConversationId(openConversationId);

        JSONObject msgParam = new JSONObject();
        msgParam.put("content", "java-getting-start say : " + text);
        orgGroupSendRequest.setMsgParam(msgParam.toJSONString());
        try {
            OrgGroupSendResponse orgGroupSendResponse = robotClient.orgGroupSendWithOptions(orgGroupSendRequest,
                    orgGroupSendHeaders, new com.aliyun.teautil.models.RuntimeOptions());
            if (Objects.isNull(orgGroupSendResponse) || Objects.isNull(orgGroupSendResponse.getBody())) {
                log.error("RobotGroupMessagesService_send orgGroupSendWithOptions return error, response={}",
                        orgGroupSendResponse);
                return null;
            }
            return orgGroupSendResponse.getBody().getProcessQueryKey();
        } catch (TeaException e) {
            log.error("RobotGroupMessagesService_send orgGroupSendWithOptions throw TeaException, errCode={}, " +
                    "errorMessage={}", e.getCode(), e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("RobotGroupMessagesService_send orgGroupSendWithOptions throw Exception", e);
            throw e;
        }
    }

    public void sendPrivateMessage(ChatbotMessage chatbotMessage) {
        logger.info("RobotGroupMessagesService_sendPrivateMessage chatbotMessage={}", chatbotMessage.getText().getContent());
        //1代表私聊，2群聊。群聊需要单独根据另外的机器人回复，本身的应用机器人不支持回复包括@
        if (Objects.equals(chatbotMessage.getConversationType(), "1")) {
            sendprivaterobot(chatbotMessage);
        } else {
            DingDingRobotParam dingDingRobotParam = assembleParam(chatbotMessage);
            HttpUtil.post(assembleParamUrl(dingDingRobotAccount), JSONObject.toJSONString(dingDingRobotParam));
        }

    }

    private void sendprivaterobot(ChatbotMessage chatbotMessage) {
        BatchSendOTOHeaders batchSendOTOHeaders = new BatchSendOTOHeaders();
        String content=getAnwserContent(chatbotMessage);
        batchSendOTOHeaders.xAcsDingtalkAccessToken = accessTokenService.getAccessToken();
        JSONObject msgParam = new JSONObject();
        msgParam.put("text", content);
        BatchSendOTORequest batchSendOTORequest = new BatchSendOTORequest()
                .setRobotCode(robotCode)
                .setUserIds(java.util.Arrays.asList(
                        chatbotMessage.getSenderStaffId()
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

    private String getAnwserContent(ChatbotMessage chatbotMessage) {
        String senderStaffId = chatbotMessage.getSenderStaffId();
        List<OpenAiApi.ChatCompletionMessage> messages = ChatHistory.get(senderStaffId);
        if(messages.size()==0){
            messages.add(createSystem(chatbotMessage));
        }
        else {
            messages.add(CreateUser(chatbotMessage));
        }
        String result=callLLM(senderStaffId,messages);

        return result;
    }

    private String callLLM(String senderStaffId,List<OpenAiApi.ChatCompletionMessage> messages) {
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
        String jsonString = ParserToolFactory.createParserTool(moduleName).parseJson(fullContent);
        OpenAiApi.ChatCompletionMessage assistantMessage = MessageUtils.createAssistantMessage(jsonString);
        messages.add(assistantMessage);
        ChatHistory.put(senderStaffId, messages);
        cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(jsonString);
        if(Objects.equals("信息提取",jsonObject.getStr("intent"))){
            Discussion discussion =  JSONUtil.toBean(jsonObject.getStr("entities"), Discussion.class);
            if(discussion.getRemark()!=null&&!Objects.equals("",discussion.getRemark())){
               return "当前提取信息如下\n"+discussion.toString()+"\n"+discussion.getRemark();
            }
            ChatHistory.invalidate(senderStaffId);
            return "当前提取信息如下\n"+discussion+"\n"+"已保存数据库";

        }
        return jsonObject.getJSONObject("entities").getStr("answer");

    }

    private OpenAiApi.ChatCompletionMessage CreateUser(ChatbotMessage chatbotMessage) {
        return MessageUtils.createUserMessage(chatbotMessage.getText().getContent());
    }

    private OpenAiApi.ChatCompletionMessage createSystem(ChatbotMessage chatbotMessage) {
        String message = StrUtils.readByResource("informationExt.txt");
        Map<String, String> varValue = new HashMap<>();
        varValue.put("input",chatbotMessage.getText().getContent());
        varValue.put("dateTime", DateUtils.getCurrentTime());
        String systemMessage = MessageUtils.createMessage(varValue, message);
        return MessageUtils.createSystemMessage(systemMessage);
    }

    private void sendprivate(ChatbotMessage chatbotMessage) {
        try {
        OapiMessageCorpconversationAsyncsendV2Request request = assemblePrivateParam(dingDingWorkNoticeAccount, chatbotMessage);
        String accessToken = accessTokenService.getAccessToken();

            new DefaultDingTalkClient(SEND_URL).execute(request, accessToken);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private OapiMessageCorpconversationAsyncsendV2Request assemblePrivateParam(DingDingWorkNoticeAccount dingDingWorkNoticeAccount, ChatbotMessage chatbotMessage) {
        OapiMessageCorpconversationAsyncsendV2Request req = new OapiMessageCorpconversationAsyncsendV2Request();

        Set<String> receives = new HashSet<>();
        receives.add(chatbotMessage.getSenderStaffId());
        req.setUseridList(StringUtils.join(receives, String.valueOf(StrUtil.C_COMMA)));
        req.setAgentId(Long.parseLong(dingDingWorkNoticeAccount.getAgentId()));
        OapiMessageCorpconversationAsyncsendV2Request.Msg message = new OapiMessageCorpconversationAsyncsendV2Request.Msg();
        message.setMsgtype(SendMessageType.TEXT.getDingDingWorkType());
        OapiMessageCorpconversationAsyncsendV2Request.Text textObj = new OapiMessageCorpconversationAsyncsendV2Request.Text();
        textObj.setContent("收到");
        message.setText(textObj);
        req.setMsg(message);
        return req;

    }

    private DingDingRobotParam assembleParam(ChatbotMessage chatbotMessage) {
        // 接收者相关
        DingDingRobotParam.AtVO atVo = DingDingRobotParam.AtVO.builder().build();
        Set<String> receives = new HashSet<>();
        receives.add(chatbotMessage.getSenderStaffId());
        atVo.setAtUserIds(new ArrayList<>(receives));
        String content = getAnwserContent(chatbotMessage);
        DingDingRobotParam param = DingDingRobotParam.builder().at(atVo)
                .msgtype(SendMessageType.TEXT.getDingDingRobotType()).build();
        param.setText(DingDingRobotParam.TextVO.builder().content(content).build());
        return param;
    }

    private String assembleParamUrl(DingDingRobotAccount account) {
        long currentTimeMillis = System.currentTimeMillis();
        String sign = assembleSign(currentTimeMillis, account.getSign());
        return (account.getWebhook() + "&timestamp=" + currentTimeMillis + "&sign=" + sign);
    }

    /**
     * 使用HmacSHA256算法计算签名
     *
     * @param currentTimeMillis
     * @param secret
     * @return
     */
    private String assembleSign(long currentTimeMillis, String secret) {
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

}
