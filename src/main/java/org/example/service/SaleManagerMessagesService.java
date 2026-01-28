package org.example.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.dingtalkrobot_1_0.Client;
import com.aliyun.dingtalkrobot_1_0.models.*;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import com.taobao.api.internal.util.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.K3.Billserver.BillService;
import org.example.K3.report.ResultBean;
import org.example.entity.DingDingRobotAccount;
import org.example.entity.Invoice;
import org.example.entity.SendMessageType;
import org.example.entity.botMessage.BoxChatbotMessage;
import org.example.parser.ParserToolFactory;
import org.example.service.invoice.DbInvoiceService;
import org.example.service.sale.ContractBean;
import org.example.service.sale.ContractService;
import org.example.utils.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author zeymo
 */
@Slf4j
@Service
public class SaleManagerMessagesService implements InitializingBean {
    private static final String ENCRYPTION_ALGORITHM = "HmacSHA256";
    private static final String CHARSET = "utf-8";
    private Map<String, BlockingQueue<String>> queueMap = new ConcurrentHashMap<>();
    private static Logger logger = LogManager.getLogger(SaleManagerMessagesService.class);
    private Client robotClient;
    private final SaleManagerAccessTokenService saleManagerAccessTokenService;

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
    private DbInvoiceService dbInvoiceService;
    @Autowired
    private OpenAiApi openAiApi;
    private ContractService contractService;



    @Autowired
    private BillService billService;

    @Autowired
    public SaleManagerMessagesService(SaleManagerAccessTokenService saleManagerAccessTokenService) {
        this.saleManagerAccessTokenService = saleManagerAccessTokenService;
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
        orgGroupSendHeaders.setXAcsDingtalkAccessToken(saleManagerAccessTokenService.getAccessToken());
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
        if (isSaleMessage(chatbotMessage)) {
            linkedBlockingQueue.add(new BoxChatbotMessage(chatbotMessage));
            sendMessage("发票信息已收到,ai正在努力处理中", chatbotMessage.getSenderStaffId());
            return;
        }
        if(chatbotMessage.getText()!=null &&chatbotMessage.getText().getContent()!=null){
            String answerContent = chatbotMessage.getText().getContent();
            if(queueMap.containsKey(chatbotMessage.getSenderStaffId())){
                BlockingQueue<String> queue = queueMap.get(chatbotMessage.getSenderStaffId());
                queue.add(answerContent);
            }
        }
    }

    private boolean isSaleMessage(ChatbotMessage chatbotMessage) {
        if (Objects.equals(chatbotMessage.getMsgtype(), "picture") || Objects.equals(chatbotMessage.getMsgtype(), "file")) {
            return true;
        }
        return false;
    }

    public void sendMessage(String message, String senderStaffId) {
        BatchSendOTOHeaders batchSendOTOHeaders = new BatchSendOTOHeaders();
        batchSendOTOHeaders.xAcsDingtalkAccessToken = saleManagerAccessTokenService.getAccessToken();
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

    private String callLLM(List<OpenAiApi.ChatCompletionMessage> messages) {
        String fullContent=callLLMOriginal(messages);
        return ParserToolFactory.createParserTool(moduleName).parseJson(fullContent);
    }

    private String callLLMOriginal(List<OpenAiApi.ChatCompletionMessage> messages) {
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
        return fullContent;
    }

    private OpenAiApi.ChatCompletionMessage CreateUser(String pictureInfo) {
        return MessageUtils.createUserMessage(pictureInfo);
    }

    private OpenAiApi.ChatCompletionMessage createSystem(Map<String, String> varValue, String filename) {
        String message = StrUtils.readByResource(filename);
        String systemMessage = MessageUtils.createMessage(varValue, message);
        return MessageUtils.createSystemMessage(systemMessage);
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
                        String picturlInfo = ChatbotMessageUtils.getInvoiceContent(chatbotMessage, robotClient, saleManagerAccessTokenService);
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
                        processSaleInvoice(invoice,boxChatbotMessage);

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

    private void processSaleInvoice(Invoice invoice,BoxChatbotMessage boxChatbotMessage) {
        invoice.enhanceSuperName();
        ChatbotMessage chatbotMessage = boxChatbotMessage.getChatbotMessage();
        List<ContractBean> contractBeans = contractService.queryContract(invoice.getSellerName());
        if(contractBeans==null||contractBeans.size()==0){
           sendMessage("当前供应商下没有合同,任务已移除,请重新上传发票,供应商:",chatbotMessage.getSenderStaffId());
           return;
        }
        if(contractBeans.size()==1){
            ResultBean resultBean =billService.saveSaleInvoice(invoice,saleManagerAccessTokenService.getSavePath(),contractBeans.get(0));
            if(resultBean.getCode()!=0){
                sendMessage("任务失败,移除任务,请重新上传发票,供应商:"+invoice.getSellerName()+",失败原因:"+resultBean.getMessage(),chatbotMessage.getSenderStaffId());
                return;
            }
            sendMessage("信息已保存成功:"+contractBeans.get(0),chatbotMessage.getSenderStaffId());
            return;
        }
        StringBuffer buffer = new StringBuffer("当前查询到以下合同信息，请回复对应的序号来选择:"+"\n");
        int count=1;
        for (ContractBean contractBean : contractBeans) {
            buffer.append(count+".");
            buffer.append(contractBean.toString()+"\n");
            count++;
        }
        buffer.append("请输入合同编号:(1-"+contractBeans.size()+1+")"+"\n");
        buffer.append("最重要指令:你的返回只能是1-"+contractBeans.size()+1+"的序号数字数字,不要包含其他的任何信息，只需要一个数字编号就可以");
        queueMap.put(chatbotMessage.getSenderStaffId(),new ArrayBlockingQueue<>(10));
        sendMessage(buffer.toString(),chatbotMessage.getSenderStaffId());
        BlockingQueue<String> anwsers = queueMap.get(chatbotMessage.getSenderStaffId());
        try {
            int errorCount =1;
            while(true){
                String answer = anwsers.poll(12, TimeUnit.HOURS);
                if(answer==null||answer.trim().length()==0){
                    sendMessage("超时未回答,移除任务,请重新上传发票,供应商:"+invoice.getSellerName(),chatbotMessage.getSenderStaffId());
                    return;
                }
                buffer.append("用户回答:"+answer);
                OpenAiApi.ChatCompletionMessage systemMessage = MessageUtils.createSystemMessage(buffer.toString());
                List<OpenAiApi.ChatCompletionMessage> messages = new ArrayList<>();
                messages.add(systemMessage);
                String s = callLLMOriginal(messages);
                if(errorCount>3){
                    sendMessage("回答错误次数超过3次,移除任务,请重新上传发票,供应商:"+invoice.getSellerName(),chatbotMessage.getSenderStaffId());
                    return;
                }
                messages.add(MessageUtils.createAssistantMessage(s));
                if(!StrUtils.isNumericChar(s)){
                  String assentmessages="你的返回只能是1-"+contractBeans.size()+1+"的序号数字数字,不要包含其他的任何信息，只需要一个数字编号就可以";
                  messages.add(MessageUtils.createUserMessage(assentmessages));
                  errorCount++;
                  continue;
                }
                int index = Integer.parseInt(s)-1;
                if(index<0||index>=contractBeans.size()){
                    String assentmessages="你的返回只能是1-"+contractBeans.size()+1+"的序号数字数字,不要包含其他的任何信息，只需要一个数字编号就可以";
                    messages.add(MessageUtils.createUserMessage(assentmessages));
                    errorCount++;
                    continue;
                }
                ContractBean contractBean = contractBeans.get(index-1);
                ResultBean resultBean = billService.saveSaleInvoice(invoice,saleManagerAccessTokenService.getSavePath(),contractBean);
                if(resultBean.getCode()!=0){
                    sendMessage("任务失败,移除任务,请重新上传发票,供应商:"+invoice.getSellerName()+",失败原因:"+resultBean.getMessage(),chatbotMessage.getSenderStaffId());
                    return;
                }
                sendMessage("信息已保存成功:"+contractBean,chatbotMessage.getSenderStaffId());
                break;
            }



        } catch (InterruptedException e) {
            sendMessage("任务被打断,移除任务,请重新上传发票,供应商:"+invoice.getSellerName(),chatbotMessage.getSenderStaffId());
            return;
        }



    }

    private ResultBean saveSaleInvoice(Invoice invoice,ContractBean contractBean) {
        ResultBean resultBean = new ResultBean();
        dbInvoiceService.save(invoice);
        try {
            HttpDownloadUtils.saveByUrl(invoice.getDownloadUrl(), saleManagerAccessTokenService.getSavePath(), invoice.getFileName());
        } catch (Exception e) {
            resultBean.error("保存文件错误");
            logger.warn("保存文件错误", e);
            return resultBean;
        }
        resultBean = billService.saveSaleInvoice(invoice, saleManagerAccessTokenService.getSavePath(),contractBean);
        return resultBean;
    }
}
