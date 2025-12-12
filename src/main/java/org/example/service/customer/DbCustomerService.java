package org.example.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DbCustomerService implements  CustomerService{
   /* @Autowired
    private JdbcTemplate jdbcTemplate;*/
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
        /*String sql = "select FcustName from  OP_Customer where FcustName=?";
        List<String> names = jdbcTemplate.query(sql,(rs, rowNum) -> rs.getString("FcustName"),customer);
        return names;*/
        List<String> names=new ArrayList<>();
        if(customNames.contains(customer)){
            names.add(customer);
            return names;
        }
        if(customer.contains("欧软")){
            names.add("苏州殴软有限公司");
        }
        return  names;
    }

    @Override
    public List<String> queryByVagueName(String customer) {
        /* String sql2 = "select TOP 5 FcustName from OP_Customer where FcustName like '%"+customer+"%'";
         return  jdbcTemplate.query(sql2,(rs, rowNum) -> rs.getString("
         FcustName"));*/
        List<String> names=new ArrayList<>();
        names.add("南京公司1");
        names.add("南京公司2");
        names.add("南京公司3");
        names.add("南京公司4");
        return  names;
    }
    @Override
    public List<Projectbean> queryProject(String customer) {
        /* String sql2 = "select TOP 5 FcustName from OP_Customer where FcustName like '%"+customer+"%'";
         return  jdbcTemplate.query(sql2,(rs, rowNum) -> rs.getString("
         FcustName"));*/
        List<Projectbean> names=new ArrayList<>();
        Projectbean projectbean1= new Projectbean();
        projectbean1.setName("项目1");
        projectbean1.setNumber("pj0001");
        names.add(projectbean1);
        if(!customer.contains("殴软")){
            Projectbean projectbean2 = new Projectbean();
            projectbean2.setName("项目2");
            projectbean2.setNumber("pj0002");
            names.add(projectbean2);
        }
        return  names;
    }
}
