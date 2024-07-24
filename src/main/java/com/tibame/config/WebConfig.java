package com.tibame.config;

import com.tibame.authentication.filter.AdminLoginInterceptor;
import com.tibame.authentication.filter.CompanyLoginInterceptor;
import com.tibame.authentication.filter.TokenParsingInterceptor;
import com.tibame.authentication.filter.UserLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@ComponentScan(
        value = "com.tibame",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = Controller.class
        )
)
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private TokenParsingInterceptor tokenParsingInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 靜態資源配置  之後要修改路徑
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 跨域請求設定 未來確定規格後要修正
        registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
    }

    @Bean
    public CommonsMultipartResolver multipartResolver() {
        // 檔案上傳依賴
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(10485760); // 10MB
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 解析token的攔截器
        registry.addInterceptor(tokenParsingInterceptor)
                .addPathPatterns("/**");

        registry.addInterceptor(new UserLoginInterceptor())
                .addPathPatterns("/user/**");   // TODO: 討論後修改

        registry.addInterceptor(new CompanyLoginInterceptor())
                .addPathPatterns("/company/**");   // TODO: 討論後修改

        registry.addInterceptor(new AdminLoginInterceptor())
                .addPathPatterns("/admin/**");  // TODO: 討論後修改
    }
}
