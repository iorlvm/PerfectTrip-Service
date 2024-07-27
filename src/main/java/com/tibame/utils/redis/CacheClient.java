package com.tibame.utils.redis;

import com.tibame.utils.basic.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


@Slf4j
@Component
public class CacheClient {
    private static final Long LOCK_TTL = 10L;
    private static final Long RETRY_INTERVAL = 50L;
    private static final Long MAX_RETRY_COUNT = LOCK_TTL * 1000 / RETRY_INTERVAL;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;


    public CacheClient(StringRedisTemplate stringRedisTemplate, TransactionTemplate transactionTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 將數據存到Redis資料庫中 並設立一般過期時間
     * 後續可以在過期時間上增加一定範圍的隨機數, 避免大量數據同時過期造成緩存雪崩
     *
     * @param key   物件的key
     * @param value 物件的value
     * @param time  過期時間
     * @param unit  時間單位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 將數據存到Redis資料庫中 並設立邏輯過期時間
     * 此筆資料不會在Redis中過期消失, 主要使用RedisData二次封裝物件設立過期時間
     * 後續依靠Redis的記憶體淘汰機制或是手動移除
     *
     * @param key   物件的key
     * @param value 物件的value
     * @param time  過期時間
     * @param unit  時間單位
     */
    public void setWithLogicExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public void setWithLogicAndNaturalExpire(String key, Object value, Long dataTTL, Long naturalTTL, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(dataTTL)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData), naturalTTL, unit);
    }


    /**
     * 互斥鎖以及邏輯過期並用自然淘汰機制的整合方案
     *
     * @param keyPrefix  key的前綴 (與id組成完整的物件key)
     * @param lockPrefix lock的前綴 (與id組成完整鎖key)
     * @param id         物件id
     * @param type       物件類型
     * @param dataTTL    資料的過期時間
     * @param statusTTL  狀態的過期時間
     * @param naturalTTL 緩存自然淘汰消失的時間
     * @param unit       時間單位
     * @param dbFallback 當Redis查詢失敗後, 搜尋SQL的函式
     * @param <R>        回傳值的類型 (根據傳入的class決定)
     * @param <ID>       id的類型 (根據傳入的id決定)
     * @return 查詢結果
     */
    public <R, ID> R queryWithMutexAndLogicExpire(
            String keyPrefix,
            String lockPrefix,
            ID id,
            Class<R> type,
            Long dataTTL,
            Long statusTTL,
            Long naturalTTL,
            TimeUnit unit,
            Function<ID, R> dbFallback
    ) {
        String key = keyPrefix + id;
        String lockKey = lockPrefix + id;

        // 從Redis中查詢緩存
        String json = stringRedisTemplate.opsForValue().get(key);
        while (true) {
            if (json != null && json.trim().isEmpty()) {
                // 查到的東西是"", 表示目前SQL中沒有這筆資料
                return null;
            } else if (json == null) {
                // Redis中找不到資料, 開始建立緩存 (互斥鎖方案)
                if (!acquireLock(lockKey)) {
                    // 超出重試上限, 還是沒有獲得鎖 (在目前設定下, 代表空轉了10秒)
                    throw new RuntimeException("Unable to acquire lock after " + MAX_RETRY_COUNT + " retries");
                }

                // 獲取鎖成功
                try {
                    // 重新確認資料是否已經重建完成
                    json = stringRedisTemplate.opsForValue().get(key);
                    if (json != null) {
                        // 確認資料已經重建, 重新進行一次分支判定
                        continue;
                    }

                    // 確認資料尚未重建, 開始查詢資料庫並建立緩存
                    R r = dbFallback.apply(id);
                    if (r == null) {
                        // 資料庫沒有這筆資料, 將空字串存入緩存
                        set(key, "", statusTTL, unit);
                    } else {
                        // 將數據存入Redis, 並設立邏輯過期 以及 自然淘汰時間
                        setWithLogicAndNaturalExpire(key, r, dataTTL, naturalTTL, unit);
                    }
                    return r;
                } finally {
                    unlock(lockKey);
                }
            } else {
                // Redis中存在這筆資料, 開始判斷檔案是否過期 (邏輯過期方案)
                RedisData redisData = JSONUtil.toBean(json, RedisData.class);

                if (redisData.getExpireTime().isBefore(LocalDateTime.now())) {
                    // 確認資料已過期: 嘗試上鎖更新資料
                    if (tryLock(lockKey)) {
                        // 成功上鎖  開啟一個新執行緒更新資料
                        CACHE_REBUILD_EXECUTOR.submit(() -> {
                            try {
                                // 查詢資料庫
                                R r = transactionTemplate.execute(status -> dbFallback.apply(id));
                                if (r == null) {
                                    // 資料庫中這筆檔案消失了(可能被刪除), 改存入空字串
                                    set(key, "", statusTTL, unit);
                                } else {
                                    // 將資料寫入Redis
                                    setWithLogicAndNaturalExpire(key, r, dataTTL, naturalTTL, unit);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            } finally {
                                unlock(lockKey);
                            }
                        });
                    }
                }

                Object data = redisData.getData();
                return JSONUtil.toBean(data, type);
            }
        }
    }

    /**
     * 利用互斥鎖解決緩存穿透與緩存擊穿, 適用於一般業務流程
     *
     * @param keyPrefix  key的前綴 (與id組成完整的物件key)
     * @param lockPrefix lock的前綴 (與id組成完整鎖key)
     * @param id         物件id
     * @param type       物件類型
     * @param time       時間
     * @param unit       時間單位
     * @param dbFallback 當Redis查詢失敗後, 搜尋SQL的函式
     * @param <R>        回傳值的類型 (根據傳入的class決定)
     * @param <ID>       id的類型 (根據傳入的id決定)
     * @return 查詢結果
     */
    public <R, ID> R queryWithMutex(String keyPrefix, String lockPrefix, ID id, Class<R> type, Long time, TimeUnit unit, Function<ID, R> dbFallback) {
        String key = keyPrefix + id;
        String lockKey = lockPrefix + id;
        try {
            while (true) {
                // 從Redis查詢緩存
                String json = stringRedisTemplate.opsForValue().get(key);
                if (json != null && !json.trim().isEmpty()) {
                    // 資料存在於Redis中, 將結果直接返回
                    return JSONUtil.toBean(json, type);
                } else if ("".equals(json)) {
                    // 查到的東西是"", 表示目前SQL中沒有這筆資料
                    return null;
                }

                // 緩存重建: 獲取互斥鎖
                if (!tryLock(lockKey)) {
                    // 鎖定失敗: 休眠一段時間重試
                    Thread.sleep(50);
                    // 原本是使用遞迴  但覺得會可能造成棧溢出  改成while自旋 (感覺後面還可以增加重試上限避免死鎖)
                } else {
                    // 成功獲取互斥鎖: 二次確認是否有其他人已經重建完緩存
                    // 如果有其他人已經重建緩存 回傳重建後緩存並解鎖 (兩段幾乎一樣的程式碼看起來好煩躁)
                    json = stringRedisTemplate.opsForValue().get(key);
                    if (json != null && !json.trim().isEmpty()) {
                        // 資料存在於Redis中  將結果直接返回
                        unlock(lockKey);  // 手動釋放鎖 (沒解10秒後鎖也會自動失效, 但還是要記得解鎖)
                        return JSONUtil.toBean(json, type);
                    } else if ("".equals(json)) {
                        // 查到的東西是"", 表示目前SQL中沒有這筆資料
                        unlock(lockKey);  // 手動釋放鎖 (沒解10秒後鎖也會自動失效, 但還是要記得解鎖)
                        return null;
                    }
                    break;
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            // 鎖獲取成功 且二次確認數據不存在
            // 執行緩存重建: 去SQL查詢 並寫入Redis
            R r = dbFallback.apply(id);
            if (r == null) {
                // 資料庫中不存在這筆資料 將空值寫入Redis 返回null
                this.set(key, "", time, unit);
                return null;
            }
            // 寫入Redis
            this.set(key, r, time, unit);
            // 返回查詢結果
            return r;
        } finally {
            // 釋放互斥鎖 (這裡才可以用finally釋放鎖)
            // 上方用finally釋放的話, 會變成一獲得鎖就直接釋放
            unlock(lockKey);
        }
    }

    // 邏輯過期方案使用的執行緒池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @PreDestroy
    public void shutdownExecutorService() {
        // 在關閉前, 先結束所有的執行緒工作 (避免jdbc連線未中止的錯誤)
        CACHE_REBUILD_EXECUTOR.shutdown();
        try {
            if (!CACHE_REBUILD_EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
                CACHE_REBUILD_EXECUTOR.shutdownNow();
                if (!CACHE_REBUILD_EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("ExecutorService did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            CACHE_REBUILD_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 利用邏輯過期解決緩存擊穿方案
     * 通常適用於活動, 須預先手動添加資料到Redis緩存
     *
     * @param keyPrefix  key的前綴 (與id組成完整的物件key)
     * @param lockPrefix lock的前綴 (與id組成完整鎖key)
     * @param id         物件id
     * @param type       物件類型
     * @param time       時間
     * @param unit       時間單位
     * @param dbFallback 當Redis查詢失敗後, 搜尋SQL的函式
     * @param <R>        回傳值的類型 (根據傳入的class決定)
     * @param <ID>       id的類型 (根據傳入的id決定)
     * @return 查詢結果
     */
    public <R, ID> R queryWithLogicExpire(String keyPrefix, String lockPrefix, ID id, Class<R> type, Long time, TimeUnit unit, Function<ID, R> dbFallback) {
        String key = keyPrefix + id;
        // 從Redis查詢緩存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 不存在預先存入的資料(表示非活動商店) 直接返回null
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        // 存在 : 判斷是否已經過期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);

        if (redisData.getExpireTime().isBefore(LocalDateTime.now())) {
            // 資料已過期: 嘗試上鎖更新資料
            String lockKey = lockPrefix + id;
            if (tryLock(lockKey)) {
                // 成功上鎖  開啟一個新執行緒更新資料
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    try {
                        // 查詢資料庫
                        R r = transactionTemplate.execute(status -> dbFallback.apply(id));
                        // 寫入Redis
                        this.setWithLogicExpire(key, r, time, unit);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        unlock(lockKey);
                    }
                });
            }
        }
        // 返回數據 (雖然可能是過期資料 但實務上不會是過期很久的資料)
        // 由於是二次封裝的類型 無法直接取得R物件 需進行二次轉換
        Object data = redisData.getData();
        return JSONUtil.toBean(data, type);
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "lock", LOCK_TTL, TimeUnit.SECONDS);
        return flag != null && flag;
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    private boolean acquireLock(String lockKey) {
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            if (tryLock(lockKey)) return true;
            try {
                Thread.sleep(RETRY_INTERVAL);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }
}

