package org.example.K3.Billserver;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingdee.bos.webapi.entity.OperateParam;
import com.kingdee.bos.webapi.sdk.ApiRequester;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.K3.report.ResultBean;
import org.example.entity.Invoice;
import org.example.service.K3user.K3userService;
import org.example.service.K3user.UserBean;
import org.example.service.sale.ContractBean;
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
    private static  final String saleformid="PAEZ_PaymentBill";
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
            resultBean= checkResult(jsonObject1, resultBean,invoice,filePath,api,false,formid);

        } catch (Exception e) {
            logger.warn("保存工作汇报失败", e);
            resultBean.error("保存工作汇报失败");
        }
        return resultBean;

    }

    private ResultBean checkResult(JSONObject jsonObject1, ResultBean resultBean,Invoice invoice,String filePath,K3CloudApi api,Boolean isAttach,String sourceFormid) {
        if (jsonObject1.containsKey("Result") && jsonObject1.getJSONObject("Result").containsKey("ResponseStatus")) {
            JSONObject reponse = jsonObject1.getJSONObject("Result").getJSONObject("ResponseStatus");
            if (Boolean.TRUE.equals(reponse.getBool("IsSuccess"))) {
                if(!isAttach){
                    JSONObject result1 = jsonObject1.getJSONObject("Result");
                    invoice.setId(result1.getStr("Id"));
                    return attachFile(invoice, api,filePath,sourceFormid);
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

    private ResultBean attachFile(Invoice invoice, K3CloudApi api,String filePath,String sourceFormid) {
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
                request.put("FormId", sourceFormid);
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
                resultBean= checkResult(resultObject, resultBean,invoice,filePath,api,true,sourceFormid);
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

    public ResultBean saveSaleInvoice(Invoice invoice, String filePath, ContractBean contractBean) {
        String json = "{\"NeedUpDateFields\": [],\"NeedReturnFields\": [],\"IsDeleteEntry\": \"true\",\"SubSystemId\": \"\",\"IsVerifyBaseDataField\": \"false\",\"IsEntryBatchFill\": \"true\",\"ValidateFlag\": \"true\",\"NumberSearch\": \"true\",\"IsAutoAdjustField\": \"true\",\"InterationFlags\": \"\",\"IgnoreInterationFlag\": \"\",\"IsControlPrecision\": \"false\",\"ValidateRepeatJson\": \"true\",\"Model\": {\"FID\": 0,\"FCreatorId\": {\"FUserID\": 16394},\"FCreateDate\": \"2026-01-22 09:32:33\",\"F_PAEZ_ConAmount\": 10000.0000000000,\"FEmp\": {\"FSTAFFNUMBER\": \"BDRY201810220002\"},\"F_PAEZ_Department\": {\"FNumber\": \"OPKHCG\"},\"F_PAEZ_ConDate\": \"2025-12-30 00:00:00\",\"F_PAEZ_ContractBillNo\": \"SH-JY20251120\",\"F_PAEZ_PaymentDate\": \"2026-01-22 00:00:00\",\"F_PAEZ_Supplier\": {\"FNUMBER\": \"VEN00861\"},\"F_UNW_PayAmount\": 1060.0,\"F_UNW_InvoiceNo\": \"1212\",\"F_UNW_FPAmount\": 1060.0,\"F_UNW_FPNotaxAmount\": 1000.0,\"F_UNW_FPtaxAmount\": 60.0,\"F_UNW_ConText\": \"迈科条码服务外包\",\"FBillHead_Link\": [{\"FBillHead_Link_FRuleId \": \"65ba355b-ab6e-40a1-824d-ab40d6041e72\",\"FBillHead_Link_FSTableName \": \"UNW_t_Cust110053\",\"FBillHead_Link_FSBillId \": \"100007\",\"FBillHead_Link_FSId \": \"100007\"}],\"F_PAEZ_SubHeadEntity\": {}}}";
        JSONObject jsonObject = JSONUtil.parseObj(json);
        UserBean userBean = k3userService.queryUserByDdingdingName(invoice.getReportName());
        ResultBean resultBean = new ResultBean();
        if (userBean == null) {
            resultBean.error("当前钉钉用户名,无法匹配k3用户,钉钉名:" + invoice.getReportName());
            return resultBean;
        }
        String currentDate = formatter.format(LocalDateTime.now());
        try {
            JSONObject model = jsonObject.getJSONObject("Model");
            JSONObject fCreatorId = model.getJSONObject("FCreatorId");
            fCreatorId.putOpt("FUserID", userBean.getfSecUserID());
            model.putOpt("FCreatorId", fCreatorId);
            model.putOpt("FCreateDate", currentDate);
            model.putOpt("F_PAEZ_ConAmount",contractBean.getF_PAEZ_ConAmount());
            JSONObject FEmp = model.getJSONObject("FEmp");
            FEmp.putOpt("FSTAFFNUMBER", contractBean.getfSTAFFNUMBER());
            model.putOpt("FEmp", FEmp);
            JSONObject f_PAEZ_Department = model.getJSONObject("F_PAEZ_Department");
            f_PAEZ_Department.putOpt("FNumber", userBean.getFdepartmentNumber());
            model.putOpt("F_PAEZ_Department", f_PAEZ_Department);
            model.putOpt("F_PAEZ_ConDate",contractBean.getF_PAEZ_ConDate());
            model.putOpt("F_PAEZ_ContractBillNo",contractBean.getF_PAEZ_ContractBillNo());
            model.putOpt("F_PAEZ_PaymentDate",currentDate.substring(0,10));
            JSONObject f_PAEZ_Supplier = model.getJSONObject("F_PAEZ_Supplier");
            f_PAEZ_Supplier.putOpt("FNUMBER", contractBean.getF_PAEZ_Supplier_FNumbe());
            model.putOpt("F_PAEZ_Supplier", f_PAEZ_Supplier);
            JSONObject fModifierld = new JSONObject();
            fModifierld.putOpt("FUserID", userBean.getfSecUserID());
            model.putOpt("FModifierId",fModifierld);
            model.putOpt("F_UNW_PayAmount",  invoice.getTotalTax() + invoice.getTotalAmount());
            model.putOpt("F_UNW_InvoiceNo",invoice.getInvoiceNumConfirm());
            model.putOpt("F_UNW_FPAmount",  invoice.getTotalTax() + invoice.getTotalAmount());
            model.putOpt("F_UNW_FPNotaxAmount",  invoice.getTotalAmount());
            model.putOpt("F_UNW_FPtaxAmount",  invoice.getTotalTax() );
            model.putOpt("F_UNW_ConText",contractBean.getF_UNW_ConText());
            JSONArray jsonArray = new JSONArray();
            JSONObject resultObject = new JSONObject();
            resultObject.putOpt("FBillHead_Link_FRuleId", contractBean.getfBillHead_Link_FRuleId());
            resultObject.putOpt("FBillHead_Link_FSTableName", contractBean.getfBillHead_Link_FSTableName());
            resultObject.putOpt("FBillHead_Link_FSBillId", contractBean.getfBillHead_Link_FSBillId());
            resultObject.putOpt("FBillHead_Link_FSId", contractBean.getfBillHead_Link_FSId());
            jsonArray.add(resultObject);
            model.putOpt("FBillHead_Link", jsonArray);
            jsonObject.putOpt("Model", model);
            K3CloudApi api = new K3CloudApi();
            String result = null;
            try {
                logger.info("保存票据:{},formId:{}", jsonObject.toString(),saleformid);
                result = api.save(saleformid, jsonObject.toString());
                logger.info("票据返回信息" + result);
                JSONObject jsonObject1 = JSONUtil.parseObj(result);
                ApiRequester.K3CloudContext.set(userBean.getUserName());
                api = new K3CloudApi();
                logger.info("当前用户:" + ApiRequester.K3CloudContext.get());
                resultBean= checkResult(jsonObject1, resultBean,invoice,filePath,api,false,saleformid);
                String date="{\"CreateOrgId\": 0,\"Numbers\": [],\"Ids\": \"110632\",\"SelectedPostId\": 0,\"UseOrgId\": 0,\"NetworkCtrl\": \"\",\"IgnoreInterationFlag\": \"\"}";
                JSONObject datejson = JSONUtil.parseObj(date);
                JSONObject model1 = new JSONObject();
                JSONObject fModifier = new JSONObject();
                fModifierld.putOpt("FUserID", userBean.getfSecUserID());
                model1.putOpt("FModifierId",fModifier);
                datejson.putOpt("Model",model1);
                datejson.putOpt("Ids", invoice.getId());
                logger.info("提交票据:{},formId:{}", datejson.toString(),saleformid);

                String submit = api.submit(saleformid, datejson.toString());
                logger.info("票据提交返回信息:" + submit);
            } catch (Exception e) {
                logger.warn("保存工作汇报失败", e);
                resultBean.error("保存工作汇报失败");
            }
            return resultBean;
        }finally {
            ApiRequester.K3CloudContext.remove();
        }

    }
    public static void main(String[] args) throws Exception {

        String json="{\"CreateOrgId\": 0,\"Numbers\": [],\"Ids\": \"110636\",\"SelectedPostId\": 0,\"UseOrgId\": 0,\"NetworkCtrl\": \"\",\"IgnoreInterationFlag\": \"\"}";
        K3CloudApi api = new K3CloudApi();
        ApiRequester.K3CloudContext.set("杜明闪");
        String submit = api.submit(saleformid, json);
        ApiRequester.K3CloudContext.remove();
        System.out.println("票据提交返回信息:" + submit);
    }
}
