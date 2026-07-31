package com.kb.youngly.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import org.springframework.context.annotation.Import;

/**
 * 루트 애플리케이션 컨텍스트 설정.
 *
 * DataSource, MyBatis, 트랜잭션, 서비스 계층 빈을 담당한다.
 * 웹(Controller) 계층 빈은 {@link ServletConfig} 에서 등록한다.
 *
 * [INFO] 설정 값은 application.properties 에서 읽는다.
 * 비밀번호, API Key 등 민감 정보는 application-secret.properties 에 두며
 * 이 파일은 .gitignore 로 제외되어 있다.
 */
@Import({SecurityConfig.class})
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {
        "com.kb.youngly",
})
@MapperScan(basePackages = {"com.kb.youngly.mapper"})
@PropertySource(value = {"classpath:/application.properties"})
public class RootConfig {

    @Value("${jdbc.driver}")
    private String driver;

    @Value("${jdbc.url}")
    private String url;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    @Value("${hikari.maximumPoolSize:10}")
    private int maximumPoolSize;

    @Value("${hikari.connectionTimeout:30000}")
    private long connectionTimeout;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * [INFO] @Value 의 플레이스홀더를 해석하기 위한 후처리기.
     * static 메서드로 선언해야 다른 빈보다 먼저 생성된다.
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setPoolName("YounglyHikariPool");
        return new HikariDataSource(config);
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactory = new SqlSessionFactoryBean();
        sqlSessionFactory.setConfigLocation(
                applicationContext.getResource("classpath:/mybatis-config.xml"));
        sqlSessionFactory.setDataSource(dataSource());
        // [INFO] XML 매퍼는 Mapper 인터페이스와 동일한 패키지 경로에 둔다.
        //        예) src/main/resources/com/kb/youngly/mapper/UserMapper.xml
        sqlSessionFactory.setMapperLocations(
                applicationContext.getResources("classpath:/com/kb/youngly/mapper/*.xml"));
        return sqlSessionFactory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }
}
