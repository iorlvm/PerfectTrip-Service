package com.tibame.utils.redis;

import com.tibame.entity.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class ImageCacheClient {
    // 圖片緩存工具設計思路:
    //      考量將byte[]轉為字串儲存會造成的額外開銷, 選擇單獨實現一個工具類別
    // 緩存策略:
    //      使用邏輯過期 (因為圖片不太會頻繁進行更新 且沒有很高的時效性問題)
    // 具體實現的細節:　
    //      整合互斥鎖方案, 緩存無資料時不會直接返回null, 而是去sql搜尋資料並存入Redis緩存
    // 更新思路:
    //      後續考慮到邏輯過期只在過期後的下次訪問時檢查圖片狀態並更新, 可能會發生有一張圖片在資料庫中已經過期很久了
    //      但過期後有好一段時間一直沒人訪問, 可能會讓使用者讀取到超久以前圖片的情形, 決定對緩存增加一個自然淘汰機制作為對策
    // 具體實現:
    //      設定一個較長的過期時間(例如: 30分鐘, 甚至24小時),
    //      每次更新緩存重置時間, 過期後超過一段時間無人讀取, 自然淘汰消失
    //      同時避免大量長時間無人讀取的圖片佔用內存

    private static final Long LOCK_TTL = 10L;

    private final static byte STATUS_NO_CACHE = 0;
    private final static byte STATUS_NO_IMAGE = 1;

    private final RedisTemplate<String, byte[]> redisTemplateForImage;
    private final StringRedisTemplate stringRedisTemplate;
    private final TransactionTemplate transactionTemplate;

    public ImageCacheClient(RedisTemplate<String, byte[]> redisTemplateForImage, StringRedisTemplate stringRedisTemplate, TransactionTemplate transactionTemplate) {
        this.redisTemplateForImage = redisTemplateForImage;
        this.stringRedisTemplate = stringRedisTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 設立不開啟緩存的狀態碼
     *
     * @param key key
     */
    public void setStatusNoCache(String key) {
        redisTemplateForImage.execute(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations operations) throws DataAccessException {
                // 開啟redis事務, 降低網路開銷
                operations.multi();
                operations.persist(key);
                operations.opsForHash().put(key, "data", new byte[]{STATUS_NO_CACHE});
                operations.opsForHash().delete(key, "mimetype", "expireTime");  // 移除狀態碼不需要的欄位, 避免佔用記憶體
                operations.exec();
                return null;
            }
        });
    }

    /**
     * 設立找不到此張圖片的狀態碼
     *
     * @param key key
     */
    public void setStatusNoImage(String key, Long statusTTL, TimeUnit unit) {
        redisTemplateForImage.execute(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations operations) throws DataAccessException {
                // 開啟redis事務, 降低網路開銷
                operations.multi();
                operations.persist(key);
                operations.opsForHash().put(key, "data", new byte[]{STATUS_NO_IMAGE});
                operations.opsForHash().delete(key, "mimetype", "expireTime");  // 移除狀態碼不需要的欄位, 避免佔用記憶體
                operations.expire(key, statusTTL, unit);
                operations.exec();
                return null;
            }
        });
    }

    /**
     * 將數據存到Redis資料庫中 並設立邏輯過期時間 (建立圖片緩存時使用)
     *
     * @param key        key
     * @param data       圖片byte陣列
     * @param mimetype   資料型態
     * @param dataTTL    過期時間
     * @param naturalTTL 自然淘汰時間
     * @param unit       時間單位
     */
    public void setWithLogicExpire(String key, byte[] data, String mimetype, Long dataTTL, Long naturalTTL, TimeUnit unit) {
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(unit.toSeconds(dataTTL));
        String expireTimeStr = String.valueOf(expireTime);

        Map<String, byte[]> map = new HashMap<>();
        map.put("data", data);
        map.put("mimetype", mimetype.getBytes(StandardCharsets.UTF_8));
        map.put("expireTime", expireTimeStr.getBytes(StandardCharsets.UTF_8));

        redisTemplateForImage.execute(new SessionCallback<Void>() {
            @Override
            public Void execute(RedisOperations operations) throws DataAccessException {
                // 開啟redis事務, 確保putAll是原子性操作(主) 並降低網路開銷(副)
                operations.multi();
                operations.persist(key);
                operations.opsForHash().putAll(key, map);
                if (naturalTTL > 0) operations.expire(key, naturalTTL, unit);
                operations.exec();
                return null;
            }
        });
    }

    // TODO: 考慮之後變成一個執行緒的service接管所有的執行緒配置
    // 更新圖片使用的執行序池
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
     * 整合互斥鎖以及邏輯過期方案解決緩存穿透與緩存擊穿, 用於圖片緩存
     *
     * @param keyPrefix     key的前綴 (與id組成完整的物件key)
     * @param lockPrefix    lock的前綴 (與id組成完整鎖key)
     * @param id            物件id
     * @param dataTTL       資料過期時間
     * @param statusTTL     狀態過期時間
     * @param naturalTTL    自然淘汰時間
     * @param unit          時間單位
     * @param dbFallback    當Redis查詢失敗後, 搜尋SQL的函式
     * @return 查詢結果
     */
    public Image queryWithMutexAndLogicExpire(
            String keyPrefix,
            String lockPrefix,
            Long id,
            Long dataTTL,
            Long statusTTL,
            Long naturalTTL,
            TimeUnit unit,
            Function<Long, Image> dbFallback
    ) {
        String key = keyPrefix + id;
        String lockKey = lockPrefix + id;

        Map<Object, Object> entries = redisTemplateForImage.opsForHash().entries(key);
        byte[] data = (byte[]) entries.get("data");
        if (data == null) {
            // 查詢不到資料, 需要去資料庫取得數據更新
            // 嘗試獲取互斥鎖
            while (!tryLock(lockKey)) {
                try {
                    // 取得鎖失敗 (已經有其他人在進行緩存重建)
                    // 休眠50毫秒以後, 重新嘗試取得鎖
                    // TODO: 考慮是否要增加重試次數上限?
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            // 成功獲取鎖
            // 再重新查詢一次緩存, 確認是否緩存已經被重建
            entries = redisTemplateForImage.opsForHash().entries(key);
            data = (byte[]) entries.get("data");
            if (data != null) {
                // 確認緩存已被重建, 遞迴一次自己 (因確定 data != null, 所以不會變成無限遞迴)
                unlock(lockKey);
                return queryWithMutexAndLogicExpire(keyPrefix, lockPrefix, id, dataTTL, statusTTL, naturalTTL, unit, dbFallback);
            }

            try {
                // 確認緩存尚未建立, 開始進行資料緩存
                Image image = dbFallback.apply(id);
                if (image == null) {
                    // 資料庫查詢不到這張圖片, 將狀態碼存入redis緩存 (防止連續請求打入資料庫)
                    // 曾經考慮過不設定過期時間, 並在圖片上傳時檢查redis是否有對應id的狀態碼 (如果有就刪除)
                    // 但考量到有風險, 還是決定使用一般過期時間的策略儲存狀態 (也許可以設定較長的過期時間?)
                    // 風險: 因為後續系統都不會二次call資料庫檢查狀態 可能會變成後來有這張圖但redis一直以為沒有
                    setStatusNoImage(key, statusTTL, TimeUnit.SECONDS);
                } else if (!image.isCacheEnabled()) {
                    // 查詢到資料 但不開啟緩存  將狀態碼存入redis緩存 (避免每次讀取不緩存的圖片都需要排隊讀取)
                    // 這個狀態不設定過期時間, 下方db查詢回傳前檢查緩存狀態是否更改
                    setStatusNoCache(key);
                } else {
                    // 查詢到資料 且開啟緩存機制, 將資料存入redis緩存
                    data = image.getData();
                    String mimetype = image.getMimetype();
                    setWithLogicExpire(key, data, mimetype, dataTTL, naturalTTL, unit);
                }
                return image;
            } finally {
                // 確保離開時解鎖
                unlock(lockKey);
            }
        } else if (data.length == 1) {
            // 查詢到資料, 但資料長度為1 (不可能構成一張合法的圖片)
            // 將其設計為狀態碼
            switch (data[0]) {
                case STATUS_NO_CACHE:
                    // 資料庫有這張圖片, 但不開啟圖片緩存 (直接呼叫dbFallback取得資料)
                    Image image = dbFallback.apply(id);

                    // 回傳前重新確認圖片狀態與緩存設定
                    if (image == null) {
                        // 找不到這張圖片(可能被刪除, 但因為某些因素redis資料沒有同步刪掉狀態碼), 修改原本的狀態碼
                        setStatusNoImage(key, statusTTL, TimeUnit.SECONDS);
                    } else if (image.isCacheEnabled()) {
                        // 緩存狀態變為開啟, 嘗試獲取鎖
                        if (tryLock(lockKey)) {
                            // 獲取鎖成功, 將資料存入redis緩存
                            data = image.getData();
                            String mimetype = image.getMimetype();
                            setWithLogicExpire(key, data, mimetype, dataTTL, naturalTTL, unit);
                            unlock(lockKey);
                        }
                        // 沒獲取到鎖, 表示有其他人已經在重建緩存了 (不需要做任何事情)
                    }
                    return image;
                case STATUS_NO_IMAGE: // 資料庫沒有這張圖片
                default:
                    return null;
            }
        } else {
            // 從緩存中查詢到資料
            String mimetype = new String((byte[]) entries.get("mimetype"), StandardCharsets.UTF_8);
            String expireString = new String((byte[]) entries.get("expireTime"), StandardCharsets.UTF_8);
            // 當查詢到過期時間為null的時候強制視為過期 (實際邏輯上不太可能會發生)
            LocalDateTime expireTime = LocalDateTime.parse(expireString);

            if (expireTime.isBefore(LocalDateTime.now()) && tryLock(lockKey)) {
                // 資料過期且成功獲取鎖, 開啟新執行緒去更新圖片
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    try {
                        // 查詢資料庫 (新執行緒無法傳遞事務, 手動增加新的事務管理)
                        Image image = transactionTemplate.execute(status -> dbFallback.apply(id));
                        // 根據查詢結果設計對應的處理方式
                        if (image == null) {
                            // 查詢不到圖片
                            setStatusNoImage(key, statusTTL, TimeUnit.SECONDS);
                        } else if (!image.isCacheEnabled()) {
                            // 圖片緩存狀態改為關閉
                            setStatusNoCache(key);
                        } else {
                            // 更新圖片的緩存資料
                            setWithLogicExpire(key, image.getData(), image.getMimetype(), dataTTL, naturalTTL, unit);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        unlock(lockKey);
                    }
                });
            }

            // 直接將舊的圖片回傳給客戶端 (因開啟緩存的圖片不具有高一致性要求)
            Image image = new Image();
            image.setData(data);
            image.setMimetype(mimetype);
            return image;
        }
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "lock", LOCK_TTL, TimeUnit.SECONDS);
        return flag != null && flag;
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}