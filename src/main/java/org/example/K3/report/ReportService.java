package org.example.K3.report;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kingdee.bos.webapi.sdk.K3CloudApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.entity.Discussion;
import org.example.service.K3user.K3userService;
import org.example.service.K3user.UserBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ReportService {
    private static Logger logger = LogManager.getLogger(ReportService.class);
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Autowired
    private K3userService k3userService;
    public ResultBean  saveReport(Discussion discussion) {
        String json="{\"NeedUpDateFields\": [],\"NeedReturnFields\": [],\"IsDeleteEntry\": \"true\",\"SubSystemId\": \"\",\"IsVerifyBaseDataField\": \"false\",\"IsEntryBatchFill\": \"true\",\"ValidateFlag\": \"true\",\"NumberSearch\": \"true\",\"IsAutoAdjustField\": \"true\",\"InterationFlags\": \"\",\"IgnoreInterationFlag\": \"\",\"IsControlPrecision\": \"false\",\"ValidateRepeatJson\": \"true\",\"Model\": {\"FID\": 0,\"Freporttype\": {\"FNUMBER\": \"01\"},\"F_PAEZ_Outside\": \"N\",\"FDate\": \"2025-12-12 00:00:00\",\"Fdepartment\": {\"FNumber\": \"OP107\",\"Fisoutside\": \"Y\",\"FCreatorId\": {\"FUserID\":\"API2888822\"},\"FReport\": \"测试开发接口功能\",\"FWorkingday\": 0.5,\"FWorkingday1\": 0.0,\"FWorkingdayCK\": 0.0,\"FWorkingdayYX\": 0.0,\"FTrain\": 0.0,\"FAmountBus\": 0.0,\"FAirplane\": 0.0,\"FAmountHotel\": 0.0,\"FMeal\": 0.0,\"FAmountOther\": 0.0,\"FAmountCoach\": 0.0,\"FDriveAmount\": 0.0,\"FLongwayBus\": 0.0,\"FRoad\": 0.0,\"Fsubsidiz\": 0.0,\"FAmountTaxi\": 0.0,\"FAmountAll\": 0.0,\"FAdressBE\": \"公司\",\"FCreateDate\": \"2025-12-12 11:44:33\", \"FOrgId\": {\"FNumber\": \"Opsoft\"},\"F_PAEZ_CusName\": \"巨翊医疗科技（苏州）有限公司\",\"F_PAEZ_ProName\": \"巨诩医疗科技 MES+WMS\",\"F_PAEZ_ProNumber\": \"OPSO1805105_01\",\"F_PAEZ_PM\": {\"FSTAFFNUMBER\": \"3474\"},\"F_PAEZ_LastAuditer\": {\"FSTAFFNUMBER\": \"BDRY201907030003\"},\"F_PAEZ_IsExpense\": \"0\",\"F_PAEZ_IsSDCheck\": \"0\",\"F_PAEZ_RTUnitID\": {\"FNumber\": \"rt\"}, \"F_PAEZ_IsSDCheck1\": \"0\",\"F_OPKD_Base\": {\"FUSERACCOUNT\": \"Lightning\"}}}}";
        JSONObject jsonObject = JSONUtil.parseObj(json);
        UserBean userBean = k3userService.queryUserByDdingdingName(discussion.getReportName());
        ResultBean resultBean = new ResultBean();
        if(userBean==null){
            resultBean.error("当前钉钉用户名,无法匹配k3用户,钉钉名:"+discussion.getReportName());
            return  resultBean;
        }
        String currentDate = formatter.format(LocalDateTime.now());
        JSONObject model = jsonObject.getJSONObject("Model");
        model.putOpt("FDate", discussion.getDateTime()+" 00:00:00");
        JSONObject fNumber = new JSONObject();
        fNumber.putOpt("FNumber",userBean.getFdepartmentNumber());
        model.putOpt("Fdepartment",fNumber);
        model.putOpt("Fisoutside",discussion.getIsOnSite());
        JSONObject fCreatorId = new JSONObject();
        fCreatorId.putOpt("FUserID",userBean.getfSecUserID());
        model.putOpt("FCreatorId",fCreatorId);
        model.putOpt("FReport",discussion.getDiscussion());
        model.putOpt("FWorkingday",Float.valueOf(discussion.getWorkload()));
        model.putOpt("FCreateDate",currentDate);
        model.putOpt("F_PAEZ_CusName",discussion.getCustomerName());
        model.putOpt("F_PAEZ_ProName",discussion.getProjectName());
        model.putOpt("F_PAEZ_ProNumber",discussion.getProjectNum());
        JSONObject f_PAEZ_PM = new JSONObject();
        f_PAEZ_PM.putOpt("FSTAFFNUMBER",discussion.getfPMNumber());
        model.putOpt("F_PAEZ_PM",f_PAEZ_PM);
        JSONObject f_OPKD_Base = new JSONObject();
        f_OPKD_Base.putOpt("FUSERACCOUNT",userBean.getfSecUserNo());
        model.putOpt("F_OPKD_Base",f_OPKD_Base);
        jsonObject.putOpt("Model",model);
        K3CloudApi api = new K3CloudApi();

        String result = null;
        try {
            logger.info("保存工作汇报:{}",jsonObject.toString());
            result = api.save("k5e20062fd1b44439b94eccb7e734e0a9",  jsonObject.toString());
            JSONObject jsonObject1 =JSONUtil.parseObj(result);
            if(jsonObject1.containsKey("Result")&&jsonObject1.getJSONObject("Result").containsKey("ResponseStatus")){
                JSONObject reponse=jsonObject1.getJSONObject("Result").getJSONObject("ResponseStatus");
                if(Boolean.TRUE.equals(reponse.getBool("IsSuccess"))){
                    return  resultBean;
                }
                JSONArray errors = reponse.getJSONArray("Errors");
                StringBuffer buffer =new StringBuffer("");
                for(int i=0;i<errors.size();i++){
                    buffer.append(errors.getJSONObject(i).get("FieldName")+","+errors.getJSONObject(i).getStr("Message")+"\n");
                }
                resultBean.error(buffer.toString());
            }else {
                resultBean.error("保存k3工作汇报失败");
            }
        } catch (Exception e) {
            logger.info("保存工作汇报失败",e);
            resultBean.error("保存工作汇报失败");
        }
        logger.info("返回信息:{}",result);
        return resultBean;

    }

    public static void main(String[] args) throws Exception {
        String date="{\"NeedUpDateFields\":[],\"NeedReturnFields\":[],\"IsDeleteEntry\":\"true\",\"SubSystemId\":\"\",\"IsVerifyBaseDataField\":\"false\",\"IsEntryBatchFill\":\"true\",\"ValidateFlag\":\"true\",\"NumberSearch\":\"true\",\"IsAutoAdjustField\":\"true\",\"InterationFlags\":\"\",\"IgnoreInterationFlag\":\"\",\"IsControlPrecision\":\"false\",\"ValidateRepeatJson\":\"true\",\"Model\":{\"FID\":0,\"Freporttype\":{\"FNUMBER\":\"01\"},\"F_PAEZ_Outside\":\"N\",\"FDate\":\"2025-12-14 00:00:00\",\"Fdepartment\":{\"FNumber\":\"OP107\"},\"Fisoutside\":\"Y\",\"FCreatorId\":{\"FUserID\":\"2893616\"},\"FReport\":\"进口香蕉问题\",\"FWorkingday\":1,\"FCreateDate\":\"2025-12-15 14:30:45\",\"F_PAEZ_CusName\":\"苏州捷力新能源材料有限公司\",\"F_PAEZ_ProName\":\"苏州捷力新能源MES\",\"F_PAEZ_ProNumber\":\"OPSO2112117\",\"F_PAEZ_PM\":{\"FSTAFFNUMBER\":\"3338\"},\"F_OPKD_Base\":{\"FUSERACCOUNT\":\"孙自友\"}}}";
        logger.info("发送信息:{}",date);
        K3CloudApi api = new K3CloudApi();
        String result = api.save("k5e20062fd1b44439b94eccb7e734e0a9",  date);
        logger.info("返回信息:"+result);

    }
}
