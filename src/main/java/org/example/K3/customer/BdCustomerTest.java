package org.example.K3.customer;


import java.util.*;

import com.google.gson.JsonObject;
import com.kingdee.bos.webapi.entity.*;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import com.google.gson.Gson;
import org.example.K3.common.SeqHelper;


public class BdCustomerTest {
    static String FNumber = SeqHelper.genNumber("BC");
    static String FNumber2 = SeqHelper.genNumber("BC");
    static String FName = "aukh_" + UUID.randomUUID().toString();
    static String FShortName = "aukh_" + UUID.randomUUID().toString();
    static K3CloudApi api = new K3CloudApi();


    /* 本接口用于实现客户 (BD_Customer) 的保存功能 */

    public static void Test(String[] args){
        String para = "{\"FName\":  "+"\""+FName+"\""+",\"FNumber\": "+"\""+FNumber+"\""+",\"FCreateOrgId\": {\"FNumber\": \"100\"},\"FUseOrgId\": {\"FNumber\": \"100\"},\"FCOUNTRY\": {    \"FNumber\": \"China\"},\"FINVOICETITLE\": \"zzl\",\"FCustTypeId\": {    \"FNumber\": \"KHLB001_SYS\"},\"FTRADINGCURRID\": {    \"FNumber\": \"PRE001\"},\"FInvoiceType\": \"1\",\"FTaxType\": {    \"FNumber\": \"SFL02_SYS\"},\"FPriority\": 1,\"FTaxRate\": {    \"FNumber\": \"SL02_SYS\"},\"FISCREDITCHECK\": true,\"FIsTrade\": true    }";
        Map<String, Object> map = new HashMap<>();
        map = new Gson().fromJson(para, map.getClass());
        SaveResult sRet = null;
        try {
            sRet = api.save("BD_Customer", new SaveParam<Map>(map));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Gson gson = new Gson();
        if (sRet.isSuccessfully()) {
            System.out.printf("客户保存接口: %s%n", gson.toJson(sRet.getResult()));
        } else {
            System.out.println("客户保存接口: " + gson.toJson(sRet.getResult()));
        }
    }

    /* 本接口用于实现客户 (BD_Customer) 的保存功能 */



}
