package com.tibame.config;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractDispatcherServletInitializer;

import javax.servlet.Filter;


// 這個類別寫法基本上是固定的
public class SpringInitConfig extends AbstractDispatcherServletInitializer {
    @Override
    protected WebApplicationContext createServletApplicationContext() {
        // 加載SpringMvc容器配置
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(WebConfig.class);
        return context;
    }


    @Override
    protected String[] getServletMappings() {
        // 設定要交給SpringMvc管理的路徑
        return new String[]{"/"};   //全部交給SpringMvc管理
    }

    @Override
    protected WebApplicationContext createRootApplicationContext() {
        // 加載Spring容器配置
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(DBConfig.class, SecurityConfig.class);
        return context;
    }

    @Override
    protected Filter[] getServletFilters() {
        // 指定編碼
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return new Filter[]{filter};
    }
}
