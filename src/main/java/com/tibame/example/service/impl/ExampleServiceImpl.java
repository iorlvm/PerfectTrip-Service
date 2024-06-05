package com.tibame.example.service.impl;

import com.tibame.dto.Result;
import com.tibame.entity.ExampleEntity;
import com.tibame.example.repository.ExampleDao;
import com.tibame.example.service.ExampleService;
import com.tibame.utils.redis.CacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ExampleServiceImpl implements ExampleService {
    @Autowired
    private ExampleDao userDao;

    @Autowired
    private CacheClient cacheClient;

    @Override
    public Result getAll() {
        List<ExampleEntity> allUsers = userDao.findAll();

        // 避免程式碼邏輯嵌套，適時使用反向邏輯的撰寫風格    ※不強制但建議
        if (allUsers == null || allUsers.isEmpty()) {
            // 依照邏輯這個東西不可能搜不到資料又能正常回應，但這是範例
            return Result.fail("出事啦！用戶資料被人全刪掉了啦！");
        }
        // 回傳值是List的情況 (前端有資料總筆數比較好計算分頁切換)
        return Result.ok(allUsers, (long) allUsers.size());
    }

    @Override
    public Result getById(Long id) {
//        ExampleEntity user = userDao.findById(id);
        // Redis緩存工具測試
        ExampleEntity user = cacheClient.queryWithMutex(
                "cache:user:",
                "lock:user:",
                id,
                ExampleEntity.class,
                60L,
                TimeUnit.SECONDS,
                userDao::findById
        );
        if (user == null) {
            return Result.fail("找不到該用戶");
        }
        // 回傳值是單一物件的情況
        return Result.ok(user);
    }
}