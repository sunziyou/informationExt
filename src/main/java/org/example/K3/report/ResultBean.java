package org.example.K3.report;

public class ResultBean {
    private  int code=0;
    private String message;

    public void error(String message){
        code =-1;
        this.message=message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
