package com.tibame.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.io.Serializable;

public class RedisConfig {
    @Value("${redis.hostname}")
    private String hostname;
    @Value("${redis.port}")
    private int port;
    @Value("${redis.password}")
    private String password;
    @Value("${redis.database}")
    private int database;
    @Value("${redis.pool.maxIdle}")
    private int maxIdle;
    @Value("${redis.pool.minIdle}")
    private int minIdle;
    @Value("${redis.pool.maxWaitMillis}")
    private int maxWaitMillis;
    @Value("${redis.pool.maxTotal}")
    private int maxTotal;



    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setHostName(hostname);
        jedisConnectionFactory.setPort(port);
        jedisConnectionFactory.setPassword(password);
        jedisConnectionFactory.setDatabase(database);
        jedisConnectionFactory.getPoolConfig().setMaxIdle(maxIdle);
        jedisConnectionFactory.getPoolConfig().setMinIdle(minIdle);
        jedisConnectionFactory.getPoolConfig().setMaxWaitMillis(maxWaitMillis);
        jedisConnectionFactory.getPoolConfig().setMaxTotal(maxTotal);
        return jedisConnectionFactory;
    }


    @Bean
    public RedisTemplate<String, Serializable> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Serializable> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        // 創建轉換格式
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        // Key 跟 HashKey使用String序列化
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        // Value 跟 HashValue使用轉換格式序列化
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, byte[]> redisTemplateForImage(RedisConnectionFactory factory) {
        // for 圖片緩存使用的序列化器
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);

        // Key 和 HashKey 使用 String 序列化
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());

        // Value 和 HashValue 使用 byteArray 序列化
        redisTemplate.setValueSerializer(RedisSerializer.byteArray());
        redisTemplate.setHashValueSerializer(RedisSerializer.byteArray());

        return redisTemplate;
    }


    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(factory);

        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(RedisSerializer.string());

        return redisTemplate;
    }
}
