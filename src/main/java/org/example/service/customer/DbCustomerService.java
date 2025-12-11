package org.example.service.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DbCustomerService implements  CustomerService{
    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Override
    public List<String> queryByName(String customer) {
        String sql = "select FcustName from  OP_Customer where FcustName=?";
        List<String> names = jdbcTemplate.query(sql,(rs, rowNum) -> rs.getString("FcustName"),customer);
        return names;
    }

    @Override
    public List<String> queryByVagueName(String customer) {
        String sql2 = "select TOP 5 FcustName from OP_Customer where FcustName like '%"+customer+"%'";
         return  jdbcTemplate.query(sql2,(rs, rowNum) -> rs.getString("FcustName"));
    }
}
