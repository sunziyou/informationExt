package org.example.entity;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

public class Discussion {
    private String dateTime;
    private String   customerName;
    private String discussion;
    private List<String> participants = new ArrayList<>();
    private String reportName;
    private String remark;

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getDiscussion() {
        return discussion;
    }

    public void setDiscussion(String discussion) {
        this.discussion = discussion;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    @Override
    public String toString() {
        return  "汇报人:"+ reportName +"(不可修改)\n" +
                "日期时间:" + dateTime + '\n' +
                "客户名称:" + customerName + '\n' +
                "讨论内容:" + discussion + '\n' +
                "参与人:" + getDiscussionString(participants);
    }

    private String getDiscussionString(List<String> strs) {
        return StrUtil.join(",",strs);
    }

    public boolean validate() {
        return StrUtil.isNotBlank(dateTime) && StrUtil.isNotBlank(customerName)
                && StrUtil.isNotBlank(discussion) && StrUtil.isNotBlank(remark)&&participants!=null &&participants.size()>0;
    }
}
