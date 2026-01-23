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
import org.example.K3.Billserver.BillService;
import org.example.K3.report.ResultBean;
import org.example.cache.InvoiceChatHistory;
import org.example.entity.DingDingRobotAccount;
import org.example.entity.Invoice;
import org.example.entity.SendMessageType;
import org.example.entity.botMessage.BoxChatbotMessage;
import org.example.parser.ParserToolFactory;
import org.example.service.invoice.DbInvoiceService;
import org.example.utils.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author zeymo
 */
@Slf4j
@Service
public class InvoiceManagerMessagesService implements InitializingBean {
    private static final String ENCRYPTION_ALGORITHM = "HmacSHA256";
    private static final String CHARSET = "utf-8";

    private static Logger logger = LogManager.getLogger(InvoiceManagerMessagesService.class);
    private Client robotClient;
    private final InvoiceManagerAccessTokenService invoiceManagerAccessTokenService;

    private LinkedBlockingQueue<BoxChatbotMessage> linkedBlockingQueue = new LinkedBlockingQueue<>(1000);
    private static final String SEND_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2";

    @Value("${robot.invoiceManager.code}")
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
    private DbInvoiceService dbInvoiceService;

    @Autowired
    private BillService billService;

    @Autowired
    public InvoiceManagerMessagesService(InvoiceManagerAccessTokenService accessTokenService) {
        this.invoiceManagerAccessTokenService = accessTokenService;
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
        orgGroupSendHeaders.setXAcsDingtalkAccessToken(invoiceManagerAccessTokenService.getAccessToken());
        OrgGroupSendRequest orgGroupSendRequest = new OrgGroupSendRequest();
        orgGroupSendRequest.setMsgKey("sampleText");
        orgGroupSendRequest.setRobotCode(robotCode);
        orgGroupSendRequest.setOpenConversationId(openConversationId);
        JSONObject msgParam = new JSONObject();
        msgParam.put("content", "java-getting-start say : " + text);
        orgGroupSendRequest.setMsgParam(msgParam.toJSONString());
        try {
            OrgGroupSendResponse orgGroupSendResponse = robotClient.orgGroupSendWithOptions(orgGroupSendRequest,
                    orgGroupSendHeaders, new RuntimeOptions());
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
        logger.info("RobotGroupMessagesService_sendPrivateMessage chatbotMessage={}", chatbotMessage);
        if (isInvoiceMessage(chatbotMessage)) {
            linkedBlockingQueue.add(new BoxChatbotMessage(chatbotMessage));
            sendMessage("发票信息已收到,ai正在努力处理中", chatbotMessage.getSenderStaffId());
            return;
        }
        //1代表私聊，2群聊。群聊需要单独根据另外的机器人回复，本身的应用机器人不支持回复包括@
        if (Objects.equals(chatbotMessage.getConversationType(), "1")) {
            sendprivaterobot(chatbotMessage);
        } else {
            DingDingRobotParam dingDingRobotParam = assembleParam(chatbotMessage);
            HttpUtil.post(assembleParamUrl(dingDingRobotAccount), JSONObject.toJSONString(dingDingRobotParam));
        }

    }

    private boolean isInvoiceMessage(ChatbotMessage chatbotMessage) {
        if (Objects.equals(chatbotMessage.getMsgtype(), "picture") || Objects.equals(chatbotMessage.getMsgtype(), "file")) {
            return true;
        }
        return false;
    }

    public void sendMessage(String message, String senderStaffId) {
        BatchSendOTOHeaders batchSendOTOHeaders = new BatchSendOTOHeaders();
        batchSendOTOHeaders.xAcsDingtalkAccessToken = invoiceManagerAccessTokenService.getAccessToken();
        JSONObject msgParam = new JSONObject();
        msgParam.put("text", message.replaceAll("\n", "  \n"));
        BatchSendOTORequest batchSendOTORequest = new BatchSendOTORequest()
                .setRobotCode(robotCode)
                .setUserIds(Arrays.asList(
                        senderStaffId
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

    private void sendprivaterobot(ChatbotMessage chatbotMessage) {
        String content = getAnwserContent(chatbotMessage);
        sendMessage(content, chatbotMessage.getSenderStaffId());
    }

    private String getAnwserContent(ChatbotMessage chatbotMessage) {
        String senderStaffId = chatbotMessage.getSenderStaffId();
        List<OpenAiApi.ChatCompletionMessage> messages = InvoiceChatHistory.get(senderStaffId);
        String picturlInfo = ChatbotMessageUtils.getInvoiceContent(chatbotMessage, robotClient, invoiceManagerAccessTokenService);
        String downloadUrl = null;
        if (JSONUtil.isTypeJSONObject(picturlInfo)) {
            cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(picturlInfo);
            if (jsonObject.containsKey("downloadUrl")) {
                downloadUrl = jsonObject.getStr("downloadUrl");
            }
        }
        Map<String, String> varValue = new HashMap<>();
        varValue.put("input", picturlInfo);
        varValue.put("dateTime", DateUtils.getCurrentTime());
        if (messages.size() == 0) {
            messages.add(createSystem(varValue, "invoiceExt.txt"));
        } else {
            messages.add(CreateUser(picturlInfo));
        }
        String result = callLLM(senderStaffId, messages, chatbotMessage.getSenderNick(), downloadUrl);

        return result;
    }

    private String callLLM(String senderStaffId, List<OpenAiApi.ChatCompletionMessage> messages, String nickName, String downloadUrl) {
        String jsonString = callLLM(messages);
        OpenAiApi.ChatCompletionMessage assistantMessage = MessageUtils.createAssistantMessage(jsonString);
        messages.add(assistantMessage);
        InvoiceChatHistory.put(senderStaffId, messages);
        cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(jsonString);
        if (Objects.equals("发票信息提取", jsonObject.getStr("intent"))) {
            Invoice invoice = JSONUtil.toBean(jsonObject.getStr("entities"), Invoice.class);
            if (invoice.getDownloadUrl() == null) {
                if (downloadUrl == null) {
                    logger.warn("发票信息提取失败,发票下载地址为空");
                    throw new RuntimeException("发票信息提取失败,发票下载地址为空");
                }
                invoice.setDownloadUrl(downloadUrl);
            }
            invoice.setReportName(nickName);
            if (Objects.equals("finish", invoice.getRemark())) {
                InvoiceChatHistory.invalidate(senderStaffId);
                ResultBean resultBean = saveInvoice(invoice);
                if (resultBean.getCode() != 0) {
                    return "提取信息错误:" + resultBean.getMessage();
                }
                return "当前提取信息如下\n" + invoice + "\n" + "已保存数据库";
            }
            String result = "当前提取信息如下\n" + invoice.toString() + "\n请确认信息是否正确";
            return result;
        }
        return jsonObject.getJSONObject("entities").getStr("answer");

    }

    private String callLLM(List<OpenAiApi.ChatCompletionMessage> messages) {
        OpenAiApi.ChatCompletionRequest request = new OpenAiApi.ChatCompletionRequest(messages, moduleName, 0.0d, true);
        Flux<OpenAiApi.ChatCompletionChunk> chatCompletionChunkFlux = openAiApi.chatCompletionStream(request);
        logger.info("大模型请求信息:" + messages);
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
        return ParserToolFactory.createParserTool(moduleName).parseJson(fullContent);
    }

    private ResultBean saveInvoice(Invoice invoice) {
        ResultBean resultBean = new ResultBean();
        dbInvoiceService.save(invoice);
        try {
            HttpDownloadUtils.saveByUrl(invoice.getDownloadUrl(), invoiceManagerAccessTokenService.getSavePath(), invoice.getFileName());
        } catch (Exception e) {
            resultBean.error("保存文件错误");
            logger.warn("保存文件错误", e);
            return resultBean;
        }
        resultBean = billService.saveInvoice(invoice, invoiceManagerAccessTokenService.getSavePath());
        return resultBean;
    }


    private OpenAiApi.ChatCompletionMessage CreateUser(String pictureInfo) {
        return MessageUtils.createUserMessage(pictureInfo);
    }

    private OpenAiApi.ChatCompletionMessage createSystem(Map<String, String> varValue, String filename) {
        String message = StrUtils.readByResource(filename);
        String systemMessage = MessageUtils.createMessage(varValue, message);
        return MessageUtils.createSystemMessage(systemMessage);
    }


   /* private void sendprivate(ChatbotMessage chatbotMessage) {
        try {
            OapiMessageCorpconversationAsyncsendV2Request request = assemblePrivateParam(dingDingWorkNoticeAccount, chatbotMessage);
            String accessToken = invoiceManagerAccessTokenService.getAccessToken();

            new DefaultDingTalkClient(SEND_URL).execute(request, accessToken);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }*/

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

    @Override
    public void afterPropertiesSet() {
        new Thread(() -> {
            BoxChatbotMessage boxChatbotMessage = null;
            while (true) {
                try {
                    boxChatbotMessage = linkedBlockingQueue.take();
                    if (boxChatbotMessage != null) {

                        ChatbotMessage chatbotMessage = boxChatbotMessage.getChatbotMessage();
                        if(boxChatbotMessage.getErrorCount() > 2){
                            sendMessage("无法识别发票请手工处理", chatbotMessage.getSenderStaffId());
                            continue;
                        }
                        List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>();
                        String picturlInfo = ChatbotMessageUtils.getInvoiceContent(chatbotMessage, robotClient, invoiceManagerAccessTokenService);
                        String downloadUrl = null;
                        if (JSONUtil.isTypeJSONObject(picturlInfo)) {
                            cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(picturlInfo);
                            if (jsonObject.containsKey("downloadUrl")) {
                                downloadUrl = jsonObject.getStr("downloadUrl");
                            }
                        }
                        Map<String, String> varValue = new HashMap<>();
                        varValue.put("input", picturlInfo);
                        varValue.put("dateTime", DateUtils.getCurrentTime());
                        messages.add(createSystem(varValue, "invoiceSimpleExt.txt"));
                        String s = callLLM(messages);
                        logger.info("大模型返回结果:" + s);
                        cn.hutool.json.JSONObject jsonObject = JSONUtil.parseObj(s);
                        Invoice invoice = JSONUtil.toBean(jsonObject.getStr("entities"), Invoice.class);

                        if(!invoice.check()){
                            boxChatbotMessage.error();
                            linkedBlockingQueue.add(boxChatbotMessage);
                            continue;
                        }
                        if(downloadUrl!=null&&downloadUrl.trim().length()>0){
                            invoice.setDownloadUrl(downloadUrl);
                        }
                        if (invoice.getDownloadUrl() == null||invoice.getDownloadUrl().trim().equals("")) {
                            logger.warn("发票信息提取失败,发票下载地址为空");
                            sendMessage("发票信息提取失败,发票下载地址为空", chatbotMessage.getSenderStaffId());
                            continue;

                        }

                        invoice.setReportName(chatbotMessage.getSenderNick());
                        ResultBean resultBean = saveInvoice(invoice);
                        String result = "当前提取信息如下\n" + invoice + "\n" + "已保存数据库";
                        if (resultBean.getCode() != 0) {
                            result = "提取信息错误:" + resultBean.getMessage();
                        }
                        sendMessage(result, chatbotMessage.getSenderStaffId());
                    }
                } catch (Throwable e) {
                    logger.warn("处理发票信息错误", e);
                    if (boxChatbotMessage != null) {
                        if (boxChatbotMessage.getErrorCount() > 0) {
                            ChatbotMessage chatbotMessage = boxChatbotMessage.getChatbotMessage();
                            String fileName = "";
                            if (chatbotMessage.getContent() != null && chatbotMessage.getContent().getFileName() != null) {
                                fileName = chatbotMessage.getContent().getFileName();
                            }
                            sendMessage("处理发票错误:" + fileName, chatbotMessage.getSenderStaffId());
                        } else {
                            logger.warn("处理发票信息错误,重新处理");
                            boxChatbotMessage.error();
                            linkedBlockingQueue.add(boxChatbotMessage);
                        }
                    }
                    logger.error("处理发票错误", e);
                }
            }

        }).start();
    }
}
