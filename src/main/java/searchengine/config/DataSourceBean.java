package searchengine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceBean {

    //jdbc:mysql://localhost:3306/search_engine?useUnicode=true&characterEncoding=utf8&useSSL=false&requireSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    private @Value("${spring.datasource.url}") String dbUrl;
    //root
    private @Value("${spring.datasource.password}") String dbPassword;
    //testtest
    private @Value("${spring.datasource.username}") String dbUserName;


    @Bean
    public DataSource getDataSource() {
        return DataSourceBuilder
                .create()
                .url(dbUrl)
                .username(dbUserName)
                .password(dbPassword)
                .build();
    }
}
