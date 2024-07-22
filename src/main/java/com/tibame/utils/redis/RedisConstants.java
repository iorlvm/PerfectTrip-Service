package com.tibame.utils.redis;

public class RedisConstants {
    public static final String LOGIN_USER = "login:user:";
    public static final String LOGIN_COMPANY = "login:company:";
    public static final String LOGIN_ADMIN = "login:admin:";
    public static final String LOCK_IMG = "lock:img:";
    public static final String CACHE_IMG = "cache:img:";
    public static final Long CACHE_IMG_SIZE = 51200L; // 50 * 1024 (50KB)
    public static final Long CACHE_IMG_DATA_TTL = 60L;
    public static final Long CACHE_IMG_STATUS_TTL = 30L;
    public static final Long CACHE_IMG_NATURAL_TTL = 1800L;

}
