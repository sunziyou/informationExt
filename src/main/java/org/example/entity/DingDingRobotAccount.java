package org.example.entity;


import org.example.utils.AESUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DingDingRobotAccount {
    /**
     * 密钥
     */
    @Value("${webhook.sign}")
    private String sign;

    /**
     * 自定义群机器人中的 webhook
     */
    @Value("${webhook.url}")
    private String webhook;

    public String getSign() {
        return AESUtils.decrypt(sign);
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getWebhook() {
        return webhook;
    }

    public void setWebhook(String webhook) {
        this.webhook = webhook;
    }


    @Override
    public String toString() {
        return "DingDingRobotAccount{" +
                "sign='" + "**********" + '\'' +
                ", webhook='" + webhook + '\'' +
                '}';
    }
}
