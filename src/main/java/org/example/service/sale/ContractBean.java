package org.example.service.sale;

public class ContractBean {

    private String  f_PAEZ_ContractBillNo;
    private String f_UNW_ConText;
    private String f_PAEZ_Supplier_Name;
    private String f_PAEZ_ConStatus;
    private String f_PAEZ_ConAmount;
    private String f_PAEZ_APAMOUNT;

    private String f_PAEZ_ConDate;


    private String  f_PAEZ_PaymentDate;

    private String f_PAEZ_Supplier_FNumber;

    private String fBillHead_Link_FRuleId;
    private String fBillHead_Link_FSTableName;
    private String fBillHead_Link_FSBillId;
    private String fBillHead_Link_FSId;
    @Override
    public String toString() {
        return  "合同号:" + f_PAEZ_ContractBillNo +"\n"+
                "合同内容：" + f_UNW_ConText  +"\n"+
                "供应商名称:" + f_PAEZ_Supplier_Name +"\n"+
                "合同金额:" + f_PAEZ_ConAmount+"\n"+
                "已付款金额:" + f_PAEZ_APAMOUNT+"\n"+
                "未付款金额:" + (Double.parseDouble(f_PAEZ_ConAmount) - Double.parseDouble(f_PAEZ_APAMOUNT))+"\n";
    }

    public String getF_PAEZ_ContractBillNo() {
        return f_PAEZ_ContractBillNo;
    }

    public void setF_PAEZ_ContractBillNo(String f_PAEZ_ContractBillNo) {
        this.f_PAEZ_ContractBillNo = f_PAEZ_ContractBillNo;
    }

    public String getF_UNW_ConText() {
        return f_UNW_ConText;
    }

    public void setF_UNW_ConText(String f_UNW_ConText) {
        this.f_UNW_ConText = f_UNW_ConText;
    }

    public String getF_PAEZ_Supplier_Name() {
        return f_PAEZ_Supplier_Name;
    }

    public void setF_PAEZ_Supplier_Name(String f_PAEZ_Supplier_Name) {
        this.f_PAEZ_Supplier_Name = f_PAEZ_Supplier_Name;
    }

    public String getF_PAEZ_ConStatus() {
        return f_PAEZ_ConStatus;
    }

    public void setF_PAEZ_ConStatus(String f_PAEZ_ConStatus) {
        this.f_PAEZ_ConStatus = f_PAEZ_ConStatus;
    }

    public String getF_PAEZ_ConAmount() {
        return f_PAEZ_ConAmount;
    }

    public String getF_PAEZ_ConDate() {
        return f_PAEZ_ConDate;
    }

    public void setF_PAEZ_ConDate(String f_PAEZ_ConDate) {
        this.f_PAEZ_ConDate = f_PAEZ_ConDate;
    }

    public String getF_PAEZ_PaymentDate() {
        return f_PAEZ_PaymentDate;
    }

    public void setF_PAEZ_PaymentDate(String f_PAEZ_PaymentDate) {
        this.f_PAEZ_PaymentDate = f_PAEZ_PaymentDate;
    }

    public String getF_PAEZ_Supplier_FNumbe() {
        return f_PAEZ_Supplier_FNumber;
    }

    public void setF_PAEZ_Supplier_FNumber(String f_PAEZ_Supplier_FNumber) {
        this.f_PAEZ_Supplier_FNumber = f_PAEZ_Supplier_FNumber;
    }

    public String getfBillHead_Link_FRuleId() {
        return fBillHead_Link_FRuleId;
    }

    public void setfBillHead_Link_FRuleId(String fBillHead_Link_FRuleId) {
        this.fBillHead_Link_FRuleId = fBillHead_Link_FRuleId;
    }

    public String getfBillHead_Link_FSTableName() {
        return fBillHead_Link_FSTableName;
    }

    public void setfBillHead_Link_FSTableName(String fBillHead_Link_FSTableName) {
        this.fBillHead_Link_FSTableName = fBillHead_Link_FSTableName;
    }

    public String getfBillHead_Link_FSBillId() {
        return fBillHead_Link_FSBillId;
    }

    public void setfBillHead_Link_FSBillId(String fBillHead_Link_FSBillId) {
        this.fBillHead_Link_FSBillId = fBillHead_Link_FSBillId;
    }

    public String getfBillHead_Link_FSId() {
        return fBillHead_Link_FSId;
    }

    public void setfBillHead_Link_FSId(String fBillHead_Link_FSId) {
        this.fBillHead_Link_FSId = fBillHead_Link_FSId;
    }

    public void setF_PAEZ_ConAmount(String f_PAEZ_ConAmount) {
        this.f_PAEZ_ConAmount = f_PAEZ_ConAmount;
    }

    public String getF_PAEZ_APAMOUNT() {
        return f_PAEZ_APAMOUNT;
    }

    public void setF_PAEZ_APAMOUNT(String f_PAEZ_APAMOUNT) {
        this.f_PAEZ_APAMOUNT = f_PAEZ_APAMOUNT;
    }
}
