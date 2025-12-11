package com.parking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.Cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Configuration
@EnableCaching
public class RedisConfig {
    
    private static final Logger logger = Logger.getLogger(RedisConfig.class.getName());
    
    // 直接硬编码Redis配置，避免环境变量问题
    private String redisHost = "localhost";
    private int redisPort = 6379;
    private String redisPassword = "";
    private int redisDatabase = 0;
    
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        try {
            logger.info("尝试配置Redis缓存管理器...");
            RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
            
            return RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(cacheConfig)
                    .build();
        } catch (Exception e) {
            logger.warning("Redis连接失败，回退到内存缓存: " + e.getMessage());
            // 如果Redis不可用，使用基于内存的缓存作为后备
            SimpleCacheManager cacheManager = new SimpleCacheManager();
            List<Cache> caches = Arrays.asList(
                new ConcurrentMapCache("default"),
                new ConcurrentMapCache("users"),
                new ConcurrentMapCache("reservations")
            );
            cacheManager.setCaches(caches);
            return cacheManager;
        }
    }
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        try {
            logger.info("尝试创建Redis连接工厂...");
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(redisPort);
            if (!redisPassword.isEmpty()) {
                config.setPassword(redisPassword);
            }
            config.setDatabase(redisDatabase);
            
            LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet(); // 验证配置
            return factory;
        } catch (Exception e) {
            logger.warning("Redis配置初始化失败: " + e.getMessage());
            // 返回一个基本的工厂实例，即使它可能无法连接
            return new LettuceConnectionFactory();
        }
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        try {
            logger.info("配置RedisTemplate...");
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(factory);
            
            // 设置key序列化器
            template.setKeySerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            
            // 设置value序列化器
            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
            template.setValueSerializer(jsonSerializer);
            template.setHashValueSerializer(jsonSerializer);
            
            template.afterPropertiesSet();
            return template;
        } catch (Exception e) {
            logger.warning("RedisTemplate配置失败: " + e.getMessage());
            // 返回一个基本的RedisTemplate实例
            RedisTemplate<String, Object> fallbackTemplate = new RedisTemplate<>();
            fallbackTemplate.setConnectionFactory(factory);
            return fallbackTemplate;
        }
    }
}