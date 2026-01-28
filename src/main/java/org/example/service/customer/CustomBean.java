package org.example.service.customer;

public class CustomBean {
    private String fCustName;
    private String fType;
    private String fKeyWord;

    private String fPWD;

    private String fNote;

    public String getfCustName() {
        return fCustName;
    }

    public void setfCustName(String fCustName) {
        this.fCustName = fCustName;
    }

    public String getfType() {
        return fType;
    }

    public void setfType(String fType) {
        this.fType = fType;
    }

    public String getfKeyWord() {
        return fKeyWord;
    }

    public void setfKeyWord(String fKeyWord) {
        this.fKeyWord = fKeyWord;
    }

    public String getfPWD() {
        return fPWD;
    }

    public void setfPWD(String fPWD) {
        this.fPWD = fPWD;
    }

    public String getfNote() {
        return fNote;
    }

    public void setfNote(String fNote) {
        this.fNote = fNote;
    }

    @Override
    public String toString() {
        return  "客户名称:" + fCustName +
                ", 类型:" + fType +
                ", 内容:" + fKeyWord +
                ", 密码:" + fPWD +
                ", 备注:'" + fNote ;

    }
}
