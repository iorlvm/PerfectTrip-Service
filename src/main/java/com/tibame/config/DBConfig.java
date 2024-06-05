package com.tibame.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@PropertySource("classpath:db.properties")
@EnableTransactionManagement
@ComponentScan({"com.tibame.utils", "com.tibame.example.service", "com.tibame.example.repository"})
@Import({RedisConfig.class})
public class DBConfig {
    @Value("${db.driver}")
    private String driver;
    @Value("${db.url}")
    private String url;
    @Value("${db.username}")
    private String username;
    @Value("${db.password}")
    private String password;

    // HikariCP settings
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_IDLE = 5;
    private static final long IDLE_TIMEOUT = 30000;
    private static final long CONNECTION_TIMEOUT = 20000;
    private static final long MAX_LIFETIME = 1800000;

    @Bean
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(driver);
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        hikariConfig.setMaximumPoolSize(MAX_POOL_SIZE);
        hikariConfig.setMinimumIdle(MIN_IDLE);
        hikariConfig.setIdleTimeout(IDLE_TIMEOUT);
        hikariConfig.setConnectionTimeout(CONNECTION_TIMEOUT);
        hikariConfig.setMaxLifetime(MAX_LIFETIME);

        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public LocalSessionFactoryBean getSessionFactory(DataSource dataSource) {
        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan("com.tibame.entity");
        return factoryBean;
    }

    @Bean
    public HibernateTransactionManager getTransactionManager(LocalSessionFactoryBean sessionFactoryBean) {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactoryBean.getObject());
        return transactionManager;
    }
}