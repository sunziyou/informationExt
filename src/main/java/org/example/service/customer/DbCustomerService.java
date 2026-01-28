package org.example.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DbCustomerService implements  CustomerService{
    @Autowired
    @Qualifier("localJdbc")
    private JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("k3Jdbc")
    private JdbcTemplate jdbcTemplateK3;
   List<String> customNames=new ArrayList<>();
    List<String> projectNames=new ArrayList<>();
   public DbCustomerService(){
       customNames.add("南京公司1");
       customNames.add("南京公司2");
       customNames.add("南京公司3");
       customNames.add("南京公司4");
   }

    @Override
    public List<String> queryByName(String customer) {
        String sql = "select distinct FCUSTName from  VW_CustInfo where FCUSTName=?";
        List<String> names = jdbcTemplateK3.query(sql,(rs, rowNum) -> rs.getString("FCUSTName"),customer);
        Set<String> set = new HashSet<>(names);
        return new ArrayList<>(set);
       /* List<String> names=new ArrayList<>();
        if(customNames.contains(customer)){
            names.add(customer);
            return names;
        }
        if(customer.contains("欧软")){
            names.add("苏州殴软有限公司");
        }
        return  names;*/
    }

    @Override
    public List<String> queryByVagueName(String customer) {
         String sql2 = "select TOP 7 FCUSTName from VW_CustInfo where FCUSTName like '%"+customer+"%'";
         List<String> fcustName = jdbcTemplateK3.query(sql2, (rs, rowNum) -> rs.getString("FCUSTName"));
         Set<String> set = new HashSet<>(fcustName);
         return new ArrayList<>(set);
       /* List<String> names=new ArrayList<>();
        names.add("南京公司1");
        names.add("南京公司2");
        names.add("南京公司3");
        names.add("南京公司4");
        return  names;*/
    }
    @Override
    public List<Projectbean> queryProject(String customer) {
        String sql2 = "select FConNumber as number,FProName as name,FPMNumber as fPMNumber from VW_ProInfo where FCUSTName=?";
        List<Projectbean> projectbeans = jdbcTemplateK3.query(sql2, new BeanPropertyRowMapper<>(Projectbean.class), customer);
        return  projectbeans;

    }

    @Override
    public List<CustomBean> queryCustomer(String customer) {
        String sql2 = "select FCustName as fCustName,FType as fType,FKeyWord as fKeyWord,FPWD as fPWD,FNote as fNote from vw_FCustSVInfo where fCustName like '%"+customer+"%'";
        List<CustomBean> customBeans = jdbcTemplateK3.query(sql2, new BeanPropertyRowMapper<>(CustomBean.class));
        return  customBeans;
    }
}
