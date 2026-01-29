package org.example.service.K3user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class K3userService {
    @Autowired
    @Qualifier("k3Jdbc")
    private JdbcTemplate jdbcTemplateK3;
    public UserBean queryUserByDdingdingName(String reportName){
        String sql="select FSecUserNo as fSecUserNo,FSecUserID as fSecUserID,FSecUserName as userName,FDEPTNumber as fdepartmentNumber  from VW_UserInfo where FUserNameDD=?";
        List<UserBean> query = jdbcTemplateK3.query(sql, new BeanPropertyRowMapper<>(UserBean.class), reportName);
        if(query.size()>0){
            return  query.get(0);
        }
        return  null;
    }

}
