package org.example.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.aliyun.dingtalkrobot_1_0.Client;
import com.aliyun.dingtalkrobot_1_0.models.RobotMessageFileDownloadHeaders;
import com.aliyun.dingtalkrobot_1_0.models.RobotMessageFileDownloadRequest;
import com.aliyun.dingtalkrobot_1_0.models.RobotMessageFileDownloadResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.service.InvoiceManagerAccessTokenService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatbotMessageUtils {
    private static Logger logger = LogManager.getLogger(ChatbotMessageUtils.class);
    private static AtomicInteger count=new AtomicInteger(1000);
    public static String getUserContent(ChatbotMessage chatbotMessage) {
        if (chatbotMessage.getText() == null) {
            if (Objects.equals("audio", chatbotMessage.getMsgtype())) {
                return chatbotMessage.getContent().getRecognition();
            }
            return "无法识别消息";
        }
        return chatbotMessage.getText().getContent();

    }

    public static String getInvoiceContent(ChatbotMessage chatbotMessage, Client robotClient, InvoiceManagerAccessTokenService invoiceManagerAccessTokenService) {
        if (Objects.equals(chatbotMessage.getMsgtype(), "picture")) {
            return getPictureContent(chatbotMessage, robotClient, invoiceManagerAccessTokenService);
        }
        if (chatbotMessage.getText() == null) {
            if (Objects.equals("audio", chatbotMessage.getMsgtype())) {
                return chatbotMessage.getContent().getRecognition();
            }
            return "无法识别消息";
        }
        return chatbotMessage.getText().getContent();

    }

    private static String getPictureContent(ChatbotMessage chatbotMessage, Client robotClient, InvoiceManagerAccessTokenService invoiceManagerAccessTokenService) {
        String downloadCode = chatbotMessage.getContent().getDownloadCode();
        RobotMessageFileDownloadHeaders robotMessageFileDownloadHeaders = new RobotMessageFileDownloadHeaders();
        robotMessageFileDownloadHeaders.xAcsDingtalkAccessToken = invoiceManagerAccessTokenService.getAccessToken();
        RobotMessageFileDownloadRequest robotMessageFileDownloadRequest = new RobotMessageFileDownloadRequest().setDownloadCode(downloadCode).setRobotCode(invoiceManagerAccessTokenService.getRobotCode());
        try {
            RobotMessageFileDownloadResponse robotMessageFileDownloadResponse = robotClient.robotMessageFileDownloadWithOptions(robotMessageFileDownloadRequest, robotMessageFileDownloadHeaders, new RuntimeOptions());
            String downloadUrl = robotMessageFileDownloadResponse.getBody().getDownloadUrl();
           // Thread.sleep(3000);
           // String file = HttpDownloadUtils.downloadFileToBase64(downloadUrl);
            String result = HttpDownloadUtils.getreceiptByBase64File(downloadUrl, invoiceManagerAccessTokenService.getBaiduapiKey());
            JSONObject jsonObject = JSONUtil.parseObj(result);
            JSONObject returnImange= jsonObject.getJSONArray("words_result").getJSONObject(0).getJSONObject("result");
            System.out.println(returnImange);
            logger.info("识别发票信息:"+returnImange);
            returnImange = reduceResult(returnImange);
            System.out.println(returnImange);
            returnImange.putOpt("downloadUrl",downloadUrl);

            return returnImange.toString();
        } catch (TeaException err) {
            logger.warn("解析图片失败",err);
            return "图片解析失败，请检查baidukey 是否开通";
        } catch (Exception _err) {
            logger.warn("解析图片失败",_err);
            return "图片解析失败，请检查baidukey 是否开通";
        }

    }

    private static JSONObject reduceResult(JSONObject returnImange) {
        JSONObject jsonObject = new JSONObject();
        for (String key : returnImange.keySet()) {
            Object value = returnImange.get(key);
            System.out.println(String.valueOf(value));
            if(value==null||Objects.equals("",value)||Objects.equals("[]",String.valueOf(value))){
                continue;
            }
            // 可以在这里添加过滤或处理逻辑
            // 例如：只保留特定字段或修改值的格式
            jsonObject.putOpt(key, value);
        }
        return jsonObject;
    }

    public static  synchronized String  getId(){
        int andIncrement = count.getAndIncrement();
        if(andIncrement>9998){
            count=new AtomicInteger(1000);
            return  "1000";
        }
        return String.valueOf(andIncrement);
    }
}
