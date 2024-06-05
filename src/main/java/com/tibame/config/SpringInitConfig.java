package com.tibame.config;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractDispatcherServletInitializer;

import javax.servlet.Filter;


// 這個類別寫法基本上是固定的 去網路搜尋直接複製貼上都行(?)
// 嚴格來說應該叫做Servlet容器的初始化的設定? 但我也不確定這個類別名稱夠不夠直覺
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
        context.register(DBConfig.class);
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


//// 另一種簡便型式的寫法 效果完全相同
//import org.springframework.web.filter.CharacterEncodingFilter;
//import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
//import javax.servlet.Filter;
//
//public class SpringInitConfig extends AbstractAnnotationConfigDispatcherServletInitializer {
//
//    @Override
//    protected Class<?>[] getRootConfigClasses() {
//        // 加載Spring容器配置
//        return new Class[]{DBConfig.class};
//    }
//
//    @Override
//    protected Class<?>[] getServletConfigClasses() {
//        // 加載SpringMvc容器配置
//        return new Class[]{WebConfig.class};
//    }
//
//    @Override
//    protected String[] getServletMappings() {
//        // 設定要交給SpringMvc管理的路徑
//        return new String[]{"/"}; // 全部交給SpringMvc管理
//    }
//
//    @Override
//    protected Filter[] getServletFilters() {
//        // 指定編碼
//        CharacterEncodingFilter filter = new CharacterEncodingFilter();
//        filter.setEncoding("UTF-8");
//        filter.setForceEncoding(true);
//        return new Filter[]{filter};
//    }
//}
