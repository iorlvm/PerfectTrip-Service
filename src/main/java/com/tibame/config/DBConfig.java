package com.tibame.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Properties;

import static org.hibernate.cfg.Environment.*;

@Configuration
@PropertySource("classpath:db.properties")
@EnableTransactionManagement
@ComponentScan({"com.tibame.service", "com.tibame.repository"})
public class DBConfig {
    @Value("${db.driver}")
    private String driver;
    @Value("${db.url}")
    private String url;
    @Value("${db.username}")
    private String username;
    @Value("${db.password}")
    private String password;

    @Value("${connectionPool.min_size}")
    private String poolMinSize;
    @Value("${connectionPool.max_size}")
    private String poolMaxSize;
    @Value("${connectionPool.timeout}")
    private String poolTimeout;

    @Bean
    public LocalSessionFactoryBean getSessionFactory() {
        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();

        Properties props = new Properties();

        props.put(DRIVER, driver);
        props.put(URL, url);
        props.put(USER, username);
        props.put(PASS, password);

        props.put(C3P0_MIN_SIZE, poolMinSize);
        props.put(C3P0_MAX_SIZE, poolMaxSize);
        props.put(C3P0_TIMEOUT, poolTimeout);

        factoryBean.setHibernateProperties(props);
        factoryBean.setPackagesToScan("com.tibame.entity");
        return factoryBean;
    }

    @Bean
    public HibernateTransactionManager getTransactionManager() {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(getSessionFactory().getObject());
        return transactionManager;
    }
}
