package com.parking.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 注册拦截器和其他Web MVC相关配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SimpleUserIdInterceptor simpleUserIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户ID拦截器，拦截所有请求
        // 这将为所有控制器方法提供@RequestAttribute("userId")
        registry.addInterceptor(simpleUserIdInterceptor)
                .addPathPatterns("/**") // 拦截所有路径
                .excludePathPatterns(
                        // 排除静态资源路径
                        "/images/**",           // 图片资源
                        "/static/**",           // 静态资源
                        "/css/**",              // CSS文件
                        "/js/**",               // JavaScript文件
                        "/fonts/**",            // 字体文件
                        "/favicon.ico",         // 网站图标
                        // 排除认证相关接口（登录等不需要用户ID）
                        "/api/v1/auth/**",      // 认证接口
                        "/api/v1/public/**",    // 公开接口
                        // 排除健康检查接口
                        "/health/**",           // 健康检查接口
                        // 排除首页和根路径
                        "/",                    // 根路径
                        "/index",               // 首页
                        // 排除错误页面
                        "/error",               // 错误页面
                        "/error/**"             // 错误页面子路径
                );
    }

    /**
     * 静态资源映射配置
     * 将 /images/** 映射到后端 classpath:/static/images/，用于托管前端大图资源
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(3600) // 设置缓存时间（秒）
                .resourceChain(true); // 启用资源链，提高性能
    }
}