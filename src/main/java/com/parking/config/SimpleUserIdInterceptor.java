package com.parking.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 简单的用户ID拦截器
 * 为所有请求设置默认的userId属性，解决控制器中使用@RequestAttribute("userId")的问题
 */
@Component
public class SimpleUserIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 设置默认用户ID为1（测试环境使用）
        // 这允许控制器方法使用@RequestAttribute("userId")而不会抛出异常
        request.setAttribute("userId", 1L);
        System.out.println("SimpleUserIdInterceptor: 设置默认用户ID为1");
        return true;
    }
}