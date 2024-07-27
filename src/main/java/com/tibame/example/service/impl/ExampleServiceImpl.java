package com.tibame.example.service.impl;

import com.tibame.dto.Result;
import com.tibame.entity.ExampleEntity;
import com.tibame.example.dao.ExampleDao;
import com.tibame.example.service.ExampleService;
import com.tibame.utils.redis.CacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ExampleServiceImpl implements ExampleService {
    @Autowired
    private ExampleDao userDao;

    @Autowired
    private CacheClient cacheClient;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ExampleEntity> getAll() {
        return userDao.findAll();
    }

    @Override
    public ExampleEntity getById(Long id) {
        // Redis緩存工具測試
        ExampleEntity user = cacheClient.queryWithMutexAndLogicExpire(
                "cache:user:",
                "lock:user:",
                id,
                ExampleEntity.class,
                60L,
                30L,
                1800L,
                TimeUnit.SECONDS,
                userDao::findById
        );

        return user;
    }

    @Override
    public boolean create(ExampleEntity user) {
        user.setId(null);

        return userDao.create(user);
    }

    @Override
    public boolean deleteById(Long id) {
        if (userDao.deleteById(id)) {
            stringRedisTemplate.delete("cache:user:" + id);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public ExampleEntity update(ExampleEntity user) {
        if (userDao.update(user)) {
            stringRedisTemplate.delete("cache:user:" + user.getId());
            return user;
        } else {
            return null;
        }
    }
}