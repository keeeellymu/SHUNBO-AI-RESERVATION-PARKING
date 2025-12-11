package com.parking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class WebConfig {
    
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);        // 核心线程数
        executor.setMaxPoolSize(20);         // 最大线程数
        executor.setQueueCapacity(100);      // 队列容量
        executor.setKeepAliveSeconds(60);    // 空闲线程存活时间
        executor.setThreadNamePrefix("parking-");
        return executor;
    }
}