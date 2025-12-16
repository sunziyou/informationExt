package org.example;

import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.callback.DingTalkStreamTopics;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import org.example.callback.ai.AIGraphPluginCallbackListener;
import org.example.callback.chatbot.DeliverCallbackListener;
import org.example.callback.chatbot.InvoiceManagerChatBotCallbackListener;
import org.example.utils.AESUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zeymo
 */
@Configuration
public class DingTalkStreamClientConfiguration {

    @Value("${app.deliverAppKey}")
    private String clientId;
    @Value("${app.deliverAppSecret}")
    private String clientSecret;

    @Value("${app.invoiceManagerKey}")
    private String invoiceManagerClientId;
    @Value("${app.invoiceManagerAppSecret}")
    private String invoiceManagerClientSecret;

   /* *//**
     * 配置OpenDingTalkClient客户端并配置初始化方法(start)
     *
     * @param saleCallbackListener
     * @param aiGraphPluginCallbackListener
     * @return
     * @throws Exception
     *//*
    @Bean(initMethod = "start",name="saleWorkReport")
    public OpenDingTalkClient configureStreamClient(@Autowired SaleCallbackListener saleCallbackListener,
                                                    @Autowired AIGraphPluginCallbackListener aiGraphPluginCallbackListener) throws Exception {
        // init stream client
        return OpenDingTalkStreamClientBuilder.custom()
                //配置应用的身份信息, 企业内部应用分别为appKey和appSecret, 三方应用为suiteKey和suiteSecret
                .credential(new AuthClientCredential(clientId, AESUtils.decrypt(clientSecret)))
                //注册机器人回调
                .registerCallbackListener(DingTalkStreamTopics.BOT_MESSAGE_TOPIC, saleCallbackListener)
                //注册graph api回调
                .registerCallbackListener(DingTalkStreamTopics.GRAPH_API_TOPIC, aiGraphPluginCallbackListener).build();
    }

    @Bean(initMethod = "start",name="ServiceWorkReport")
    public OpenDingTalkClient configureStreamClient(@Autowired ServiceCallbackListener serviceCallbackListener,
                                                    @Autowired AIGraphPluginCallbackListener aiGraphPluginCallbackListener) throws Exception {
        // init stream client
        return OpenDingTalkStreamClientBuilder.custom()
                //配置应用的身份信息, 企业内部应用分别为appKey和appSecret, 三方应用为suiteKey和suiteSecret
                .credential(new AuthClientCredential(clientId, AESUtils.decrypt(clientSecret)))
                //注册机器人回调
                .registerCallbackListener(DingTalkStreamTopics.BOT_MESSAGE_TOPIC, serviceCallbackListener)
                //注册graph api回调
                .registerCallbackListener(DingTalkStreamTopics.GRAPH_API_TOPIC, aiGraphPluginCallbackListener).build();
    }*/

    @Bean(initMethod = "start",name="deliverWorkReport")
    public OpenDingTalkClient configureStreamClient(@Autowired DeliverCallbackListener deliverCallbackListener,
                                                    @Autowired AIGraphPluginCallbackListener aiGraphPluginCallbackListener) throws Exception {
        // init stream client
        return OpenDingTalkStreamClientBuilder.custom()
                //配置应用的身份信息, 企业内部应用分别为appKey和appSecret, 三方应用为suiteKey和suiteSecret
                .credential(new AuthClientCredential(clientId, AESUtils.decrypt(clientSecret)))
                //注册机器人回调
                .registerCallbackListener(DingTalkStreamTopics.BOT_MESSAGE_TOPIC, deliverCallbackListener)
                //注册graph api回调
                .registerCallbackListener(DingTalkStreamTopics.GRAPH_API_TOPIC, aiGraphPluginCallbackListener).build();
    }


    @Bean(initMethod = "start",name="invoiceManager")
    public OpenDingTalkClient configureInvoiceManager(@Autowired InvoiceManagerChatBotCallbackListener invoiceManagerChatBotCallbackListener,
                                                    @Autowired AIGraphPluginCallbackListener aiGraphPluginCallbackListener) throws Exception {
        // init stream client
        return OpenDingTalkStreamClientBuilder.custom()
                //配置应用的身份信息, 企业内部应用分别为appKey和appSecret, 三方应用为suiteKey和suiteSecret
                .credential(new AuthClientCredential(invoiceManagerClientId, AESUtils.decrypt(invoiceManagerClientSecret)))
                //注册机器人回调
                .registerCallbackListener(DingTalkStreamTopics.BOT_MESSAGE_TOPIC, invoiceManagerChatBotCallbackListener)
                //注册graph api回调
                .registerCallbackListener(DingTalkStreamTopics.GRAPH_API_TOPIC, aiGraphPluginCallbackListener).build();
    }
}
