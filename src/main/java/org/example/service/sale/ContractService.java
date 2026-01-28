package org.example.service.sale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class ContractService {
    @Autowired
    @Qualifier("k3Jdbc")
    private JdbcTemplate jdbcTemplateK3;

    public List<ContractBean> queryContract(String supplierName){
        String sql = "select F_PAEZ_ContractBillNo as f_PAEZ_ContractBillNo,F_UNW_ConText as f_UNW_ConText,F_PAEZ_Supplier_Name as f_PAEZ_Supplier_Name,F_PAEZ_ConStatus as f_PAEZ_ConStatus,F_PAEZ_ConAmount as f_PAEZ_ConAmount,F_PAEZ_APAMOUNT as f_PAEZ_APAMOUNT,F_PAEZ_ConDate as f_PAEZ_ConDate,F_PAEZ_ContractDate as f_PAEZ_ContractDate,F_PAEZ_Supplier_FNumber as f_PAEZ_Supplier_FNumber,FBillHead_Link_FRuleId as fBillHead_Link_FRuleId,FBillHead_Link_FSTableName  as fBillHead_Link_FSTableName,FBillHead_Link_FSBillId  as fBillHead_Link_FSBillId,FBillHead_Link_FSId  as fBillHead_Link_FSId    from vw_FPayConInfo where F_PAEZ_Supplier_Name=?";
        List<ContractBean> contractBeans = jdbcTemplateK3.query(sql, new BeanPropertyRowMapper<>(ContractBean.class), supplierName);
        return  contractBeans;
    }


}
