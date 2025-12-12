package org.example.service;


import cn.hutool.core.text.StrPool;
import lombok.Data;
import org.example.utils.AESUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 钉钉工作消息 账号信息
 * <p>
 * AppKey和AppSecret以及agentId都可在钉钉开发者后台的应用详情页面获取。
 * <p>
 * https://open-dev.dingtalk.com/?spm=ding_open_doc.document.0.0.13b6722fd9ojfy
 *
 * @author sunzy
 */
@Data
@Component
public class DingDingWorkNoticeAccount {

    /**
     * 应用的唯一标识key。
     */
    @Value("${app.deliverAppKey}")
    private String appKey;

    /**
     * 应用的密钥
     */
    @Value("${app.deliverAppSecret}")
    private String appSecret;

    /**
     * 发送消息时使用的微应用的AgentID
     */
    @Value("${app.deliverAgentId}")
    private String agentId;

    @Override
    public String toString() {
        return "DingDingWorkNoticeAccount{" +
                "appKey='" + appKey + '\'' +
                ", appSecret='" + "************" + '\'' +
                ", agentId='" + agentId + '\'' +
                '}';
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return AESUtils.decrypt(appSecret);
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getUniKey(){
        return "dingdingWork"+ StrPool.DASHED+agentId+StrPool.DASHED+appKey;
    }
 }
