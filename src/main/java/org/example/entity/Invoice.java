package org.example.entity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class Invoice {
    private static Logger logger = LogManager.getLogger(Invoice.class);
    private String reportName;
    private String purchaserName;
    private String sellerName;
    private String invoiceNumConfirm;
    private String invoiceDate;
    private String serviceType;
    private String totalAmount;

    private String totalTax;

    private String remark;

    private String invoiceType;

    private String downloadUrl;

    public String getFileName(){
        return reportName+"_"+invoiceNumConfirm+".png";
    }
    @Override
    public String toString() {
        return  "报销人:" + reportName  + "\n"+
                "收票方:" + purchaserName+"\n"+
                "开票方:" + sellerName+"\n"+
                "发票号:" + invoiceNumConfirm+"\n"+
                "发票日期:" + invoiceDate +"\n"+
                "发票类型:" + serviceType +"\n"+
                "金额:" + totalAmount +"\n"+
                "税额:" + totalTax +"\n"+
                "发票种类:"+invoiceType;
    }

    public String getInvoiceType() {
        return invoiceType;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void setInvoiceType(String invoiceType) {
        this.invoiceType = invoiceType;
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

    public String getPurchaserName() {
        return purchaserName;
    }

    public void setPurchaserName(String purchaserName) {
        this.purchaserName = purchaserName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getInvoiceNumConfirm() {
        return invoiceNumConfirm;
    }

    public void setInvoiceNumConfirm(String invoiceNumConfirm) {
        this.invoiceNumConfirm = invoiceNumConfirm;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(String invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(String totalTax) {
        this.totalTax = totalTax;
    }
}
