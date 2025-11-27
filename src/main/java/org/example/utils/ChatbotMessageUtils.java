package org.example.utils;

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

public class ChatbotMessageUtils {
    private static Logger logger = LogManager.getLogger(ChatbotMessageUtils.class);

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
           return HttpDownloadUtils.getreceiptByBase64File(downloadUrl, invoiceManagerAccessTokenService.getBaiduapiKey());
        } catch (TeaException err) {
            logger.warn("获取图片失败",err);
        } catch (Exception _err) {
            logger.warn("获取图片失败",_err);
        }
        return "";
    }

}
