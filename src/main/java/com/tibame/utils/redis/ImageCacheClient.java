package com.tibame.utils.redis;

import com.tibame.entity.Image;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class ImageCacheClient {
    private static final Long LOCK_TTL = 10L;
    private static final Long CACHE_IMG_STATUS_TTL = 20L;

    private final static byte STATUS_NO_CACHE = 0;
    private final static byte STATUS_NO_IMAGE = 1;

    private final RedisTemplate<String, byte[]> redisTemplateForImage;
    private final StringRedisTemplate stringRedisTemplate;

    public ImageCacheClient(RedisTemplate<String, byte[]> redisTemplateForImage, StringRedisTemplate stringRedisTemplate) {
        this.redisTemplateForImage = redisTemplateForImage;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 圖片緩存工具: 考量將byte[]轉為字串儲存會造成的額外開銷, 選擇單獨實現一個工具類別
    // 緩存策略: 使用邏輯過期 (因為圖片不太會頻繁進行更新 且沒有很高的時效性問題) <- 即讀取到稍微過期的圖片不會有太大的影響
    // 具體實現的細節:　整合互斥鎖方案, 緩存無資料時不會直接返回null, 而是去sql搜尋資料並存入Redis緩存

    /**
     * 將數據存到Redis資料庫中 (設立不開啟緩存的狀態碼)
     *
     * @param key  二進制資料的key
     * @param data 二進制的圖片資料陣列
     */
    public void set(String key, byte[] data) {
        redisTemplateForImage.opsForValue().set(key, data);
    }

    /**
     * 將數據存到Redis資料庫中 並設立一般過期時間 (設立找不到此張圖片的狀態碼)
     *
     * @param key  二進制資料的key
     * @param data 二進制的圖片資料陣列
     * @param time 過期時間
     * @param unit 時間單位
     */
    public void set(String key, byte[] data, Long time, TimeUnit unit) {
        redisTemplateForImage.opsForValue().set(key, data, time, unit);
    }

    /**
     * 將數據存到Redis資料庫中 並設立邏輯過期時間 (建立圖片緩存時使用)
     *
     * @param dataKey   二進制資料的key
     * @param expireKey 過期時間的key
     * @param typeKey   資料型態的key
     * @param data      二進制的圖片資料陣列
     * @param mimetype  資料型態
     * @param time      過期時間
     * @param unit      時間單位
     */
    public void setWithLogicExpire(String dataKey, String expireKey, String typeKey, byte[] data, String mimetype, Long time, TimeUnit unit) {
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(unit.toSeconds(time));
        // TODO: 優化成具有原子性的操作 (使用lua腳本?)
        redisTemplateForImage.opsForValue().set(dataKey, data);
        stringRedisTemplate.opsForValue().set(typeKey, mimetype);
        stringRedisTemplate.opsForValue().set(expireKey, String.valueOf(expireTime));
    }

    // 更新圖片使用的執行序池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 整合互斥鎖以及邏輯過期方案解決緩存穿透與緩存擊穿, 用於圖片緩存
     *
     * @param keyPrefix  key的前綴 (與id組成完整的物件key)
     * @param lockPrefix lock的前綴 (與id組成完整鎖key)
     * @param id         物件id
     * @param time       時間
     * @param unit       時間單位
     * @param dbFallback 當Redis查詢失敗後, 搜尋SQL的函式
     * @return 查詢結果
     */
    public Image queryWithMutexAndLogicExpire(String keyPrefix, String lockPrefix, Long id, Long time, TimeUnit unit, Function<Long, Image> dbFallback) {
        String dataKey = keyPrefix + "data:" + id;
        String expireKey = keyPrefix + "expire:" + id;
        String typeKey = keyPrefix + "type:" + id;
        String lockKey = lockPrefix + id;

        byte[] data = redisTemplateForImage.opsForValue().get(dataKey);
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
            data = redisTemplateForImage.opsForValue().get(dataKey);
            if (data != null) {
                // 確認緩存已被重建, 遞迴一次自己 (因確定 data != null, 所以不會變成無限遞迴)
                unlock(lockKey);
                return queryWithMutexAndLogicExpire(keyPrefix, lockPrefix, id, time, unit, dbFallback);
            }

            try {
                // 確認緩存尚未建立, 開始進行資料緩存
                Image image = dbFallback.apply(id);
                if (image == null) {
                    // 資料庫查詢不到這張圖片, 將狀態碼存入redis緩存 (防止連續請求打入資料庫)
                    // 曾經考慮過不設定過期時間, 並在圖片上傳時檢查redis是否有對應id的狀態碼 (如果有就刪除)
                    // 但考量到有風險, 還是決定使用一般過期時間的策略儲存狀態 (也許可以設定較長的過期時間?)
                    // 風險: 因為後續系統都不會二次call資料庫檢查狀態 可能會變成後來有這張圖但redis一直以為沒有
                    // TODO: 但不管怎麼樣, 圖片上傳時還是要檢查是否有對應id的狀態碼 (修改service.save())
                    data = new byte[]{STATUS_NO_IMAGE};
                    this.set(dataKey, data, CACHE_IMG_STATUS_TTL, TimeUnit.SECONDS);
                } else if (!image.isCacheEnabled()) {
                    // 查詢到資料 但不開啟緩存  將狀態碼存入redis緩存 (避免每次讀取不緩存的圖片都需要排隊讀取)
                    // 這個狀態不設定過期時間, 下方db查詢回傳前檢查緩存狀態是否更改
                    data = new byte[]{STATUS_NO_CACHE};
                    this.set(dataKey, data);
                } else {
                    // 查詢到資料 且開啟緩存機制, 將資料存入redis緩存
                    data = image.getData();
                    String mimetype = image.getMimetype();
                    this.setWithLogicExpire(dataKey, expireKey, typeKey, data, mimetype, time, unit);
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
                        data[0] = STATUS_NO_IMAGE;
                        set(dataKey, data, CACHE_IMG_STATUS_TTL, TimeUnit.SECONDS);
                    } else if (image.isCacheEnabled()) {
                        // 緩存狀態變為開啟, 嘗試獲取鎖
                        if (tryLock(lockKey)) {
                            // 獲取鎖成功, 將資料存入redis緩存
                            data = image.getData();
                            String mimetype = image.getMimetype();
                            setWithLogicExpire(dataKey, expireKey, typeKey, data, mimetype, time, unit);
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
            String mimetype = stringRedisTemplate.opsForValue().get(typeKey);
            String expireString = stringRedisTemplate.opsForValue().get(expireKey);
            // 當查詢到過期時間為null的時候強制視為過期 (實際邏輯上不太可能會發生)
            LocalDateTime expireTime = expireString == null ? LocalDateTime.now().minusSeconds(10) : LocalDateTime.parse(expireString);

            if (expireTime.isBefore(LocalDateTime.now())) {
                // 資料過期 嘗試獲取同步鎖 開啟新執行緒去更新圖片
                if (tryLock(lockKey)) {
                    CACHE_REBUILD_EXECUTOR.submit(() -> {
                        try {
                            // 查詢資料庫
                            Image image = dbFallback.apply(id);

                            // 根據查詢結果設計對應的處理方式
                            if (image == null) {
                                // 查詢不到圖片
                                set(dataKey, new byte[]{STATUS_NO_IMAGE}, CACHE_IMG_STATUS_TTL, TimeUnit.SECONDS);

                                // mimetype, expireString延時刪除 (避免更新中有其他人取得空值)
                                if (mimetype != null) stringRedisTemplate.opsForValue().set(typeKey,mimetype,time,unit);
                                if (expireString != null) stringRedisTemplate.opsForValue().set(expireKey,expireString,time,unit);
                            } else if (!image.isCacheEnabled()) {
                                // 圖片緩存狀態改為關閉
                                this.set(dataKey, new byte[]{STATUS_NO_CACHE});

                                // mimetype, expireString延時刪除 (避免更新中有其他人取得空值)
                                if (mimetype != null) stringRedisTemplate.opsForValue().set(typeKey,mimetype,time,unit);
                                if (expireString != null) stringRedisTemplate.opsForValue().set(expireKey,expireString,time,unit);
                            } else {
                                // 更新圖片的緩存資料
                                this.setWithLogicExpire(dataKey, expireKey, typeKey, image.getData(), image.getMimetype(), time, unit);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            unlock(lockKey);
                        }
                    });
                }
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