package org.example.entity;

import cn.hutool.core.util.StrUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.service.customer.CustomerService;
import org.example.service.customer.Projectbean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Discussion {
    private static Logger logger = LogManager.getLogger(Discussion.class);
    private String dateTime;
    private String   customerName;
    private String discussion;
    private String participants;
    private String reportName;
    private String remark;
    private String  projectName;
    private String projectNum;
    private String workload;
    private String isOnSite;
    private String customerNameError;

    private String fPMNumber;

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

    public String getParticipants() {
        return participants;
    }

    public void setParticipants(String participants) {
        this.participants = participants;
    }

    public void setCustomerNameError(String customerNameError) {
        this.customerNameError = customerNameError;
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectNum() {
        return projectNum;
    }

    public void setProjectNum(String projectNum) {
        this.projectNum = projectNum;
    }

    public String getWorkload() {
        return workload;
    }

    public void setWorkload(String workload) {
        this.workload = workload;
    }

    public String getIsOnSite() {
        return isOnSite;
    }

    public void setIsOnSite(String isOnSite) {
        this.isOnSite = isOnSite;
    }

    public String getfPMNumber() {
        return fPMNumber;
    }

    public void setfPMNumber(String fPMNumber) {
        this.fPMNumber = fPMNumber;
    }

    @Override
    public String toString() {
        return  "汇报人:"+ reportName +"(不可修改)\n" +
                "日期时间:" + dateTime + '\n' +
                "客户名称:" + customerName + '\n' +
                "讨论内容:" + discussion + '\n' +
                "参与人:" + participants+ '\n' +
                "项目名称:"+projectName+ '\n' +
                "项目编号:"+projectNum+'\n' +
                "工作量:"+workload+'\n' +
                "是否现场:"+isOnSite;
    }

    public String getTipsInfo() {
        return customerNameError;
    }

    private String getDiscussionString(List<String> strs) {
        return StrUtil.join(",",strs);
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
                return valiDateProject(customerService,customerName);
            }
            names =customerService.queryByVagueName(customerName);
            if(names==null||names.size()==0){
                this.customerNameError="当前客户名称错误,请重新说明客户名称。";
                return  false;
            }
            if(names.size()==1){
                this.customerNameError=null;
                this.customerName=names.get(0);
                return valiDateProject(customerService,this.customerName);
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

    private boolean valiDateProject(CustomerService customerService,String customerName) {
        List<Projectbean> projectbeans = customerService.queryProject(customerName);
        if(projectbeans.size()==0){
            this.customerNameError="当前客户名下没有项目,公司名:"+customerName+",请选择其他客户";
            return  false;
        }
        if(containsProjectNum(projectbeans)){
            return  true;
        }
        if(projectbeans.size()==1){
            this.projectName=projectbeans.get(0).getName();
            this.projectNum=projectbeans.get(0).getNumber();
            this.fPMNumber=projectbeans.get(0).getfPMNumber();
            return  true;
        }
        StringBuffer buffer = new StringBuffer("根据客户查询到如下项目:\n");
        for(int i=0;i<projectbeans.size();i++){
            buffer.append((i+1)+". 项目名称:"+projectbeans.get(i).getName()+",项目编号:"+projectbeans.get(i).getNumber()+"\n");
        }
        buffer.append("请根据序号选择项目名称");
        this.customerNameError=buffer.toString();
        return  false;
    }

    private boolean containsProjectNum(List<Projectbean> projectbeans) {
        if(projectNum==null||Objects.equals("",projectNum)){
            return  false;
        }
        for(Projectbean projectbean:projectbeans){
            if(Objects.equals(projectbean.getNumber(),projectNum)){
                this.fPMNumber=projectbean.getfPMNumber();
                return  true;
            }
        }
        return  false;
    }

    public boolean validateProperties() {
        StringBuffer stringBuffer = new StringBuffer("\n");
        if(StrUtil.isEmpty(customerName)){
            stringBuffer.append("客户名称不能为空"+"\n");
        }
        if(StrUtil.isEmpty(discussion)){
            stringBuffer.append("汇报不能为空"+"\n");
        }
        if(StrUtil.isEmpty(dateTime)){
            stringBuffer.append("汇报时间不能为空"+"\n");
        }
        if(StrUtil.isEmpty(workload)){
            stringBuffer.append("工作量不能为空"+"\n");
        }else{
            if(!workload.matches("-?\\d+(\\.\\d+)?")){
                stringBuffer.append("工作量必须是数字类型"+"\n");
            }
        }
        if(StrUtil.isEmpty(isOnSite)){
            isOnSite="Y";
        }
        if(stringBuffer.length()>2){
            this.customerNameError=stringBuffer.toString();
            return  false;
        }
        return  true;

    }
}
