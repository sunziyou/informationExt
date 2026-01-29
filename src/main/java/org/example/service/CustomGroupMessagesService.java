package org.example.service;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.dingtalkrobot_1_0.Client;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import com.taobao.api.internal.util.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.K3.report.ReportService;
import org.example.K3.report.ResultBean;
import org.example.cache.ChatHistory;
import org.example.cache.CustomHistory;
import org.example.entity.DingDingRobotAccount;
import org.example.entity.Discussion;
import org.example.service.customer.CustomBean;
import org.example.service.customer.DbCustomerService;
import org.example.service.discussion.DbDiscussionService;
import org.example.utils.ChatbotMessageUtils;
import org.example.utils.MessageUtils;
import org.example.utils.OpenAiUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author zeymo
 */
@Slf4j
@Service
public class CustomGroupMessagesService extends AbstractMessageService {


    private static Logger logger = LogManager.getLogger(CustomGroupMessagesService.class);
    private Client robotClient;
    private final CustomAccessTokenService customAccessTokenService;
    private static final String SEND_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2";

    @Value("${robot.customerManager.code}")
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
    private ReportService reportService;
    @Autowired
    public CustomGroupMessagesService(CustomAccessTokenService customAccessTokenService) {
        this.customAccessTokenService = customAccessTokenService;
    }

    @PostConstruct
    public void init() throws Exception {
        robotClient = initClient();
    }

    public void sendPrivateMessage(ChatbotMessage chatbotMessage) {
        try {
            logger.info("RobotGroupMessagesService_sendPrivateMessage chatbotMessage={}", ChatbotMessageUtils.getUserContent(chatbotMessage));
            //1代表私聊，2群聊。群聊需要单独根据另外的机器人回复，本身的应用机器人不支持回复包括@
            if (Objects.equals(chatbotMessage.getConversationType(), "1")) {
                sendDeliverPrivateRobot(chatbotMessage);
            } else {
                String content = getAnwserContent(chatbotMessage);
                DingDingRobotParam dingDingRobotParam = assembleParam(content, chatbotMessage);
                HttpUtil.post(assembleParamUrl(dingDingRobotAccount), JSONObject.toJSONString(dingDingRobotParam));
            }

        }catch (Exception e){
            sendContentToRobotCode(customAccessTokenService.getAccessToken(), "汇报系统出错,请重新汇报", robotClient, robotCode, chatbotMessage.getSenderStaffId());
            ChatHistory.invalidate(chatbotMessage.getSenderStaffId());
            logger.warn("汇报系统出错",e);
        }

    }

    private void sendDeliverPrivateRobot(ChatbotMessage chatbotMessage) {
        String content = getAnwserContent(chatbotMessage);
        sendContentToRobotCode(customAccessTokenService.getAccessToken(), content, robotClient, robotCode, chatbotMessage.getSenderStaffId());
    }

    public String getAnwserContent(ChatbotMessage chatbotMessage) {
        String userContent = ChatbotMessageUtils.getUserContent(chatbotMessage);
        if(Objects.equals("clear",userContent.toLowerCase())){
            CustomHistory.invalidate(chatbotMessage.getSenderStaffId());
            return "历史消息已经清除";
        }

        List<OpenAiApi.ChatCompletionMessage> messages = getMessages(chatbotMessage, "user.txt");
        logger.info("请求内容:"+messages);
        String jsonString = OpenAiUtils.invokeLLm(openAiApi, messages, moduleName);
        logger.info("大模型返回:"+jsonString);
        addAssistToHistory(messages, chatbotMessage, jsonString);
        cn.hutool.json.JSONObject customJson = JSONUtil.parseObj(jsonString);
        String message = "无法获取用户名,请补充";
        if(customJson.containsKey("userName")&&!StringUtils.isEmpty(customJson.getStr("userName"))){
            String userName = customJson.getStr("userName");
            List<CustomBean> customBeans = dbCustomerService.queryCustomer(userName);
            if(customBeans.size()>0){
                StringBuffer buffer = new StringBuffer("获取到如下客户:\n");
                int count=1;
                for (CustomBean customBean : customBeans) {
                    buffer.append(count+"."+customBean.toString()).append("\n");
                    count++;
                }
                CustomHistory.invalidate(chatbotMessage.getSenderStaffId());
                return buffer.toString();
            }
            message = "无法获取用户信息，请确认用户名:"+userName+"是否正确";
        }

        OpenAiApi.ChatCompletionMessage userMessage = MessageUtils.createUserMessage(message);
        messages.add(userMessage);
        CustomHistory.put(chatbotMessage.getSenderStaffId(), messages);
        return message;
    }
    protected List<OpenAiApi.ChatCompletionMessage>  getMessages(ChatbotMessage chatbotMessage,String fileName){
        String senderStaffId = chatbotMessage.getSenderStaffId();
        List<OpenAiApi.ChatCompletionMessage> messages = CustomHistory.get(senderStaffId);
        if (messages.size() == 0) {
            messages.add(createSystem(chatbotMessage,fileName));
        } else {
            messages.add(CreateUser(chatbotMessage));
        }
        return messages;
    }
    protected void  addAssistToHistory(List<OpenAiApi.ChatCompletionMessage> messages,ChatbotMessage chatbotMessage,String jsonString){
        OpenAiApi.ChatCompletionMessage assistantMessage = MessageUtils.createAssistantMessage(jsonString);
        messages.add(assistantMessage);
        String senderStaffId=chatbotMessage.getSenderStaffId();
        CustomHistory.put(senderStaffId, messages);
    }
    private ResultBean saveDiscussion(Discussion discussion) {
        dbDiscussionService.saveDiscussion(discussion);
        ResultBean resultBean =reportService.saveReport(discussion);
        return  resultBean;
    }


    private String assembleParamUrl(DingDingRobotAccount account) {
        long currentTimeMillis = System.currentTimeMillis();
        String sign = assembleSign(currentTimeMillis, account.getSign());
        return (account.getWebhook() + "&timestamp=" + currentTimeMillis + "&sign=" + sign);
    }


}
