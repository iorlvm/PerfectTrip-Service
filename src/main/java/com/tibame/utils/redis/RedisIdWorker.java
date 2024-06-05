package com.tibame.utils.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    // 開始的時間戳 :(UTC)2024/1/1  0h0m0s
    private final static long BEGIN_TIMESTAMP = 1704067200L;

    private final static int COUNT_BITS = 32;

    public StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Long nextId(String keyPrefix) {
        LocalDateTime now = LocalDateTime.now();
        long nowSec = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSec - BEGIN_TIMESTAMP;

        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 利用特性當key不存在時會自動創建並給1
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        return timestamp << COUNT_BITS | count;
    }
}
