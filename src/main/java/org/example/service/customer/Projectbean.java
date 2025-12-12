package org.example.service.customer;

/**
 * @author ：sunziyou
 * @date ：Created in 2025/12/13 17:23
 * @modified By：
 */
public class Projectbean {
    private String name;
    private String number;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return  "项目名称:" + name +
                ",项目号:" + number ;
    }
}
