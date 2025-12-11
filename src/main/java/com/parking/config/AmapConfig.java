package com.parking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmapConfig {
    @Value("${amap.api-key}")
    private String apiKey;
    
    @Bean
    public AmapClient amapClient() {
        return new AmapClient.Builder()
                .apiKey(apiKey)
                .timeout(3000)
                .retryCount(2)
                .build();
    }
    
    // 内部类：模拟高德地图客户端
    public static class AmapClient {
        private String apiKey;
        private int timeout;
        private int retryCount;
        
        private AmapClient(Builder builder) {
            this.apiKey = builder.apiKey;
            this.timeout = builder.timeout;
            this.retryCount = builder.retryCount;
        }
        
        public static class Builder {
            private String apiKey;
            private int timeout;
            private int retryCount;
            
            public Builder apiKey(String apiKey) {
                this.apiKey = apiKey;
                return this;
            }
            
            public Builder timeout(int timeout) {
                this.timeout = timeout;
                return this;
            }
            
            public Builder retryCount(int retryCount) {
                this.retryCount = retryCount;
                return this;
            }
            
            public AmapClient build() {
                return new AmapClient(this);
            }
        }
    }
}