package com.parking.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS配置类
 * 使用FilterRegistrationBean方式配置CORS，确保最高优先级
 */
@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 重要：明确设置allowCredentials为false
        config.setAllowCredentials(false);
        
        // 允许的源模式，使用allowedOriginPatterns而非allowedOrigins
        config.addAllowedOriginPattern("*");
        
        // 允许所有HTTP方法
        config.addAllowedMethod("*");
        
        // 允许所有请求头
        config.addAllowedHeader("*");
        
        // 允许暴露的响应头
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("X-Total-Count");
        config.addExposedHeader("Authorization");
        
        // 设置预检请求缓存时间
        config.setMaxAge(3600L);
        
        // 注册到所有路径
        source.registerCorsConfiguration("/**", config);
        
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        // 设置最高优先级，确保覆盖其他可能的CORS配置
        bean.setOrder(0);
        
        return bean;
    }
}