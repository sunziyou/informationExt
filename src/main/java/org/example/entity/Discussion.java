package org.example.entity;

import cn.hutool.core.util.StrUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.service.customer.CustomerService;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Discussion {
    private static Logger logger = LogManager.getLogger(Discussion.class);
    private String dateTime;
    private String   customerName;
    private String discussion;
    private List<String> participants = new ArrayList<>();
    private String reportName;
    private String remark;

    private String customerNameError;

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

    public String getCustomerNameError() {
        return customerNameError;
    }

    private String getDiscussionString(List<String> strs) {
        return StrUtil.join(",",strs);
    }

    public boolean validate() {
        return StrUtil.isNotBlank(dateTime) && StrUtil.isNotBlank(customerName)
                && StrUtil.isNotBlank(discussion) && StrUtil.isNotBlank(remark)&&participants!=null &&participants.size()>0;
    }

    public boolean validateCustomerName(CustomerService customerService) {
        try {
            if(customerService==null){
                return true;
            }
            if(customerName==null|| Objects.equals("",customerName)){
                return false;
            }
            List<String> names = customerService.queryByName(customerName);
            if(names!=null &&names.size()==1){
                this.customerNameError=null;
                return true;
            }
            names =customerService.queryByVagueName(customerName);

            if(names==null||names.size()==0){
                this.customerNameError="当前客户名称错误,请重新说明客户名称。";
                return  false;
            }
            if(names.size()==1){
                this.customerNameError=null;
                this.customerName=names.get(0);
                return true;
            }
            StringBuffer buffer = new StringBuffer("根据当前信息查询到如下客户:\n");
            for(int i=0;i<names.size();i++){
                buffer.append((i+1)+". "+names.get(i)+"\n");
            }
            buffer.append("请根据序号选择需要的客户名称");
            this.customerNameError=buffer.toString();
            return  false;
        }catch (Exception e){
            logger.warn("校验客户名称错误",e);
            this.customerNameError="数据库访问错误";
            return  false;
        }

    }
}
