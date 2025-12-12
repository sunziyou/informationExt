package org.example.dao;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.utils.AESUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
/**
 * @Author sunzy
 * @DATE Create in  2019/12/9 10:16
 */
@Configuration
public class JdbcTemplateWapper {
    private static Logger logger = LogManager.getLogger(JdbcTemplateWapper.class);

    @Autowired
    Environment env;
    @Value("${apiKey}")
    private String apiKey;
    @Value("${baseUrl}")
    private String baseUrl;
    @Bean
    public OpenAiApi createOpenAiApi() {
        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(AESUtils.decrypt(apiKey)).baseUrl(baseUrl).build();
        return openAiApi;
    }
   /* @Bean(name="localJdbc")
    public JdbcTemplate initJdbc() {
		try {
            HikariConfig hikariConfig = new HikariConfig();
            //        hikariConfig.setJdbcUrl("jdbc:mysql://localhost:3306/mydata");//mysql
            hikariConfig.setJdbcUrl(getConfig("jdbc.url"));//oracle
            hikariConfig.setDriverClassName(getConfig("jdbc.drive"));
            hikariConfig.setUsername(getConfig("jdbc.username"));
            hikariConfig.setPassword(AESUtils.decrypt(getConfig("jdbc.password")));
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            HikariDataSource ds = new HikariDataSource(hikariConfig);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
            return jdbcTemplate;
        }catch (Exception e){
             throw   new RuntimeException(e);
        }
    }*/

    /*@Bean(name="k3Jdbc")
    public JdbcTemplate initK3Jdbc() {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            //        hikariConfig.setJdbcUrl("jdbc:mysql://localhost:3306/mydata");//mysql
            hikariConfig.setJdbcUrl(getConfig("k3jdbc.url"));//oracle
            hikariConfig.setDriverClassName(getConfig("k3jdbc.drive"));
            hikariConfig.setUsername(getConfig("k3jdbc.username"));
            hikariConfig.setPassword(AESUtils.decrypt(getConfig("k3jdbc.password")));
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "270");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2072");
            HikariDataSource ds = new HikariDataSource(hikariConfig);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
            return jdbcTemplate;
        }catch (Exception e){
            throw   new RuntimeException(e);
        }
    }*/

    private String getConfig(String key) {
        return env.getProperty(key);
    }


}
