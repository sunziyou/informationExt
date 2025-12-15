package org.example.service.discussion;

import cn.hutool.core.util.StrUtil;
import org.example.entity.Discussion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DbDiscussionService implements DiscussionService {
    @Autowired
    @Qualifier("localJdbc")
    private JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("k3Jdbc")
    private JdbcTemplate jdbcTemplateK3;

    @Override
    public int saveDiscussion(Discussion discussion) {
        String sql = "INSERT INTO OP_WorkReport (FDate, FEmpName, FCustName, FCustEmpName, FReprotContent, FCreadteDate) " +
                "VALUES (?, ?, ?, ?, ?, GETDATE())";

       int count =jdbcTemplate.update(sql,discussion.getDateTime(),discussion.getReportName(),discussion.getCustomerName(), StrUtil.join(",",discussion.getParticipants()),discussion.getDiscussion());
        return  count;
        //return 0;
    }
}
