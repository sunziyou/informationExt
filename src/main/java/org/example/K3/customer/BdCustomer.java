package org.example.K3.customer;

public class BdCustomer {
    long CustID;
    String FNumber;
    String FName;
    Long msterID;

    public long getCustID() {
        return CustID;
    }

    public void setCustID(long custID) {
        CustID = custID;
    }

    public String getFNumber() {
        return FNumber;
    }

    public void setFNumber(String FNumber) {
        this.FNumber = FNumber;
    }

    public String getFName() {
        return FName;
    }

    public void setFName(String FName) {
        this.FName = FName;
    }

    public String getFShortName() {
        return FShortName;
    }

    public void setFShortName(String FShortName) {
        this.FShortName = FShortName;
    }

    String FShortName;

    public Long getMsterID() {
        return msterID;
    }

    public void setMsterID(Long msterID) {
        this.msterID = msterID;
    }
}
