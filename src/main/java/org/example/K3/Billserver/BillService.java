package org.example.K3.Billserver;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.K3.report.ResultBean;
import org.example.entity.Invoice;
import org.example.service.K3user.K3userService;
import org.example.service.K3user.UserBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class BillService {
    private static Logger logger = LogManager.getLogger(BillService.class);
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static  final String formid="UNW_OPINvoiceSP";
    @Autowired
    private K3userService k3userService;

    public ResultBean saveInvoice(Invoice invoice,String filePath) {
        String json = "{\"NeedUpDateFields\": [],\"NeedReturnFields\": [\"id\"],\"IsDeleteEntry\": \"true\",\"SubSystemId\": \"\",\"IsVerifyBaseDataField\": \"false\",\"IsEntryBatchFill\": \"true\",\"ValidateFlag\": \"true\",\"NumberSearch\": \"true\",\"IsAutoAdjustField\": \"true\",\"InterationFlags\": \"\",\"IgnoreInterationFlag\": \"\",\"IsControlPrecision\": \"false\",\"ValidateRepeatJson\": \"true\",\"Model\": {\"FID\": 0,\"FBillNo\": \"99999121212\",\"FCreatorId\": {\"FUserID\": 101191},\"FCreateDate\": \"2025-12-16 22:07:34\",\"F_PAEZ_NonTaxAmount\": 20.0,\"F_PAEZ_Tax\": 11.0,\"F_PAEZ_TaxAmount\": 100.0,\"F_PAEZ_Department\": {\"FNumber\": \"OP107\"},\"F_PAEZ_Companykp\": \"滴滴出行\",\"F_PAEZ_InvoiceDate\": \"2025-12-16 00:00:00\",\"F_PAEZ_InvoiceName\": \"旅游运输服务\",\"F_UNW_TaxType\": \"01\",\"F_PAEZ_InvoiceType\": \"电子发票（普通发票）\",\"F_UNW_BillType\": \"01\",\"F_UNW_CompanySP\": \"江苏欧软信息科技有限公司\",\"F_UNW_EmpText\": \"杜明闪\",\"F_UNW_Emp\": {\"FSTAFFNUMBER\": \"BDRY201810220001\"},\"F_PAEZ_SubHeadEntity\": {}}}";
        JSONObject jsonObject = JSONUtil.parseObj(json);
        UserBean userBean = k3userService.queryUserByDdingdingName(invoice.getReportName());
        ResultBean resultBean = new ResultBean();
        if (userBean == null) {
            resultBean.error("当前钉钉用户名,无法匹配k3用户,钉钉名:" + invoice.getReportName());
            return resultBean;
        }
        String currentDate = formatter.format(LocalDateTime.now());
        JSONObject model = jsonObject.getJSONObject("Model");
        model.putOpt("FBillNo", invoice.getInvoiceNumConfirm());
        JSONObject fCreatorId = model.getJSONObject("FCreatorId");
        fCreatorId.putOpt("FUserID", userBean.getfSecUserID());
        model.putOpt("FCreatorId", fCreatorId);
        model.putOpt("FCreateDate", currentDate);
        model.putOpt("F_PAEZ_NonTaxAmount", invoice.getTotalAmount());
        model.putOpt("F_PAEZ_Tax", invoice.getTotalTax());
        model.putOpt("F_PAEZ_TaxAmount", invoice.getTotalTax() + invoice.getTotalAmount());
        JSONObject f_PAEZ_Department = model.getJSONObject("F_PAEZ_Department");
        f_PAEZ_Department.putOpt("FNumber", userBean.getFdepartmentNumber());
        model.putOpt("F_PAEZ_Department", f_PAEZ_Department);
        model.putOpt("F_PAEZ_Companykp", invoice.getSellerName());
        model.putOpt("F_PAEZ_InvoiceDate", invoice.getInvoiceDate() + " 00:00:00");
        model.putOpt("F_PAEZ_InvoiceName", invoice.getServiceType());
        model.putOpt("F_PAEZ_InvoiceType", invoice.getInvoiceType());
        model.putOpt("F_UNW_CompanySP", invoice.getPurchaserName());
        model.putOpt("F_UNW_EmpText", invoice.getReportName());
        JSONObject f_UNW_Emp = model.getJSONObject("F_UNW_Emp");
        f_UNW_Emp.putOpt("FSTAFFNUMBER", userBean.getfSecUserNo());
        model.putOpt("F_UNW_Emp", f_UNW_Emp);
        jsonObject.putOpt("Model", model);
        K3CloudApi api = new K3CloudApi();
        String result = null;
        try {
            logger.info("保存票据:{}", jsonObject.toString());
            result = api.save(formid, jsonObject.toString());
            logger.info("票据返回信息" + result);
            JSONObject jsonObject1 = JSONUtil.parseObj(result);
            resultBean= checkResult(jsonObject1, resultBean,invoice,filePath,api,false);

        } catch (Exception e) {
            logger.warn("保存工作汇报失败", e);
            resultBean.error("保存工作汇报失败");
        }
        return resultBean;

    }

    private ResultBean checkResult(JSONObject jsonObject1, ResultBean resultBean,Invoice invoice,String filePath,K3CloudApi api,Boolean isAttach) {
        if (jsonObject1.containsKey("Result") && jsonObject1.getJSONObject("Result").containsKey("ResponseStatus")) {
            JSONObject reponse = jsonObject1.getJSONObject("Result").getJSONObject("ResponseStatus");
            if (Boolean.TRUE.equals(reponse.getBool("IsSuccess"))) {
                if(!isAttach){
                    JSONObject result1 = jsonObject1.getJSONObject("Result");
                    invoice.setId(result1.getStr("Id"));
                    return attachFile(invoice, api,filePath);
                }
                return resultBean;
            }
            JSONArray errors = reponse.getJSONArray("Errors");
            StringBuffer buffer = new StringBuffer("");
            for (int i = 0; i < errors.size(); i++) {
                buffer.append(nullAsEmpty(errors.getJSONObject(i).get("FieldName")) + "," + errors.getJSONObject(i).getStr("Message") + "\n");
            }
            resultBean.error(buffer.toString());
        } else {
            resultBean.error("保存票据失败");
        }
        return  resultBean;
    }

    private String nullAsEmpty(Object fieldName) {
        if(fieldName==null||"null".equals(fieldName.toString().toLowerCase())){
            return "";
        }
        return  String.valueOf(fieldName);
    }

    private ResultBean attachFile(Invoice invoice, K3CloudApi api,String filePath) {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        String billNo = invoice.getInvoiceNumConfirm();
        String id = invoice.getId();
        String fullFilePath = filePath + File.separator + invoice.getFileName();
        if (filePath.endsWith("/") || filePath.endsWith("\\")) {
            fullFilePath = filePath + invoice.getFileName();
        }
        ResultBean resultBean = new ResultBean();
        int blockSize = 3*1024 * 1024; // 分块大小：3M
        File file = new File(fullFilePath);
        logger.info("文件路径:"+fullFilePath);
        if (file.length() <= 0) {
             resultBean.error("文件内容为空");
             return resultBean;
        }
        FileInputStream fis=null;
        try {
             fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            resultBean.error("文件不存在");
            return resultBean;
        }
        byte[] content = new byte[blockSize];
        String fileId = "";
        int count=0;
        while (true) {
            try {
                int size = fis.read(content);
                if (size == 0) {
                    break;
                }
                count++;
                if(count>10){
                    resultBean.error("附件太大放弃上传");
                }
                logger.info("文件读取大小:"+size);
                boolean isLast = (size != blockSize);
                logger.info("isLast:"+isLast);
                byte[] uploadBytes = Arrays.copyOf(content, size);
                String fileBase64String = Base64.encode(uploadBytes);
                Map<String, Object> request = new HashMap<>();
                request.put("FileName", invoice.getFileName());
                request.put("FormId", formid);
                request.put("IsLast", isLast);
                request.put("InterId", id);
                request.put("BillNo", billNo);
                request.put("AliasFileName", invoice.getFileName());
                request.put("FileId ", fileId);
                request.put("SendByte", fileBase64String);
                //logger.info("上传附件数据:"+JSONUtil.toJsonStr(request));
                String result = api.attachmentUpload(new Gson().toJson(request));
                logger.info("上传附件结果:"+result);
                JSONObject resultObject = JSONUtil.parseObj(result);
                JSONObject data = resultObject.getJSONObject("Result");
                resultBean= checkResult(resultObject, resultBean,invoice,filePath,api,true);
                if(resultBean.getCode()!=0){
                    return  resultBean;
                }
                // 第一个分块上传时，FileId是空，从第二个分块开始，FileId不能再为空
                fileId = data.getStr("FileId");
                if (isLast) {
                    return resultBean;
                }
            }catch (Exception e){
                logger.warn("上传附件错误",e);
                resultBean.error("上传附件错误"+e.getMessage());
                return  resultBean;
            }
        }
        return resultBean;
    }

    public static void main(String[] args) throws Exception {

        String formId = "UNW_OPINvoiceSP";
        String billNo = "25117000001389086695";
        String id = "100047";
        String fileName="滴滴电子发票.pdf";
        String filePath = "C:\\Users\\supersun\\Desktop\\发票"+ File.separator + fileName;
        ResultBean resultBean = new ResultBean();
        int blockSize = 3*1024 * 1024; // 分块大小：1M
        File file = new File(filePath);
        if (file.length() <= 0) {
            resultBean.error("文件内容为空");
           return;
        }
        FileInputStream fis=null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            resultBean.error("文件不存在");
            System.out.println(resultBean.getMessage());
            return;
        }
        byte[] content = new byte[blockSize];
        String fileId = "";
        int count=0;
        K3CloudApi api = new K3CloudApi();
        while (true) {
            try {
                int size = fis.read(content);
                if (size == 0) {
                    break;
                }
                count++;
                if(count>10){
                    resultBean.error("附件太大放弃上传");
                }
                boolean isLast = (size != blockSize);
                byte[] uploadBytes = Arrays.copyOf(content, size);
                String fileBase64String = Base64.encode(uploadBytes);
                Map<String, Object> request = new HashMap<>();
                request.put("FileName",fileName);
               /* request.put("FormId", formId);*/
                request.put("IsLast", isLast);
              /*  request.put("InterId", id);
                request.put("BillNo", billNo);*/
               /* request.put("AliasFileName", fileName);*/
                request.put("FileId ", fileId);
                logger.info("上传附件数据:"+JSONUtil.toJsonStr(request));
                request.put("SendByte", fileBase64String);
                String result = api.attachmentUpload(new Gson().toJson(request));
                logger.info("上传附件结果:"+result);
                JSONObject resultObject = JSONUtil.parseObj(result);
                JSONObject data = resultObject.getJSONObject("Result");
                // 第一个分块上传时，FileId是空，从第二个分块开始，FileId不能再为空
                fileId = data.getStr("FileId");
                if (isLast) {
                    break;
                }
            }catch (Exception e){
                logger.warn("上传附件错误",e);
                resultBean.error("上传附件错误"+e.getMessage());
                return ;
            }
        }
        return ;

    }
}
