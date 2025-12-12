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
import org.example.service.customer.DbCustomerService;
import org.example.service.discussion.DbDiscussionService;
import org.example.utils.*;
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
public class DeliverGroupMessagesService extends AbstractMessageService {


    private static Logger logger = LogManager.getLogger(DeliverGroupMessagesService.class);
    private Client robotClient;
    private final DeliverAccessTokenService deliverAccessTokenService;
    private static final String SEND_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2";

    @Value("${robot.deliver.code}")
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
    private DbCustomerService dbCustomerService;
    @Autowired
    private DbDiscussionService dbDiscussionService;

    @Autowired
    public DeliverGroupMessagesService(DeliverAccessTokenService deliverAccessTokenService) {
        this.deliverAccessTokenService = deliverAccessTokenService;
    }

    @PostConstruct
    public void init() throws Exception {
        robotClient = initClient();
    }

    public void sendPrivateMessage(ChatbotMessage chatbotMessage) {
        logger.info("RobotGroupMessagesService_sendPrivateMessage chatbotMessage={}", ChatbotMessageUtils.getUserContent(chatbotMessage));
        //1代表私聊，2群聊。群聊需要单独根据另外的机器人回复，本身的应用机器人不支持回复包括@
        if (Objects.equals(chatbotMessage.getConversationType(), "1")) {
            sendDeliverPrivateRobot(chatbotMessage);
        } else {
            String content = getAnwserContent(chatbotMessage);
            DingDingRobotParam dingDingRobotParam = assembleParam(content, chatbotMessage);
            HttpUtil.post(assembleParamUrl(dingDingRobotAccount), JSONObject.toJSONString(dingDingRobotParam));
        }

    }

    private void sendDeliverPrivateRobot(ChatbotMessage chatbotMessage) {
        String content = getAnwserContent(chatbotMessage);
        sendContentToRobotCode(deliverAccessTokenService.getAccessToken(), content, robotClient, robotCode, chatbotMessage.getSenderStaffId());
    }

    public String getAnwserContent(ChatbotMessage chatbotMessage) {
        List<OpenAiApi.ChatCompletionMessage> messages = getMessages(chatbotMessage, "workReport.txt");
        logger.info("请求内容:"+messages);
        String jsonString = OpenAiUtils.invokeLLm(openAiApi, messages, moduleName);
        logger.info("大模型返回:"+jsonString);
        addAssistToHistory(messages, chatbotMessage, jsonString);
        cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(jsonString);
        Discussion discussion = JSONUtil.toBean(jsonObject.getStr("entities"), Discussion.class);
        discussion.setReportName(chatbotMessage.getSenderNick());
        boolean validate = discussion.validateCustomerName(dbCustomerService);
        String content = "";
        if(!Objects.equals("finish",discussion.getRemark())){
            content=discussion.getRemark();
            if(content!=null&&content.length()>0){
                content+="\n";
            }
        }
        if (!validate) {
            content += discussion.getTipsInfo();
            messages.add(MessageUtils.createUserMessage(content));
            ChatHistory.put(chatbotMessage.getSenderStaffId(), messages);
            return content;
        }
        if ("finish".equals(discussion.getRemark())) {
            if (!discussion.validateProperties()) {
                content += discussion.getTipsInfo();
                messages.add(MessageUtils.createUserMessage(content));
                ChatHistory.put(chatbotMessage.getSenderStaffId(), messages);
                return content;
            }
            saveDiscussion(discussion);
            ChatHistory.invalidate(chatbotMessage.getSenderStaffId());
            content = "恭喜工作汇报已完成";
            return content;
        } else {
            content += "汇报信息如下:" + '\n' + discussion + '\n' + "请确认";
            messages.add(MessageUtils.createUserMessage(content));
            ChatHistory.put(chatbotMessage.getSenderStaffId(), messages);
        }

        return content;
    }


    private void saveDiscussion(Discussion discussion) {
        dbDiscussionService.saveDiscussion(discussion);
    }


    private String assembleParamUrl(DingDingRobotAccount account) {
        long currentTimeMillis = System.currentTimeMillis();
        String sign = assembleSign(currentTimeMillis, account.getSign());
        return (account.getWebhook() + "&timestamp=" + currentTimeMillis + "&sign=" + sign);
    }


}
