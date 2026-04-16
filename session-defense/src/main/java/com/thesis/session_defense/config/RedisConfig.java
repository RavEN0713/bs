package com.thesis.session_defense.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    // 针对我们的会话防护系统，纯字符串操作的 StringRedisTemplate 已经完全足够了，
    // 并且 Spring Boot 原生支持它，不会有任何序列化报错或依赖缺失问题。
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }
}
