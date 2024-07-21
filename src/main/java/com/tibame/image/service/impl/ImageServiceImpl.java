package com.tibame.image.service.impl;

import com.tibame.dto.ImageUploadRequest;
import com.tibame.entity.Image;
import com.tibame.image.dao.ImageDao;
import com.tibame.image.service.ImageService;
import com.tibame.utils.basic.ImageUtils;
import com.tibame.utils.redis.ImageCacheClient;
import com.tibame.utils.redis.RedisIdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.tibame.utils.redis.RedisConstants.*;

@Service
@Transactional
public class ImageServiceImpl implements ImageService {
    @Autowired
    private ImageDao imageDao;
    @Autowired
    private RedisIdWorker idWorker;
    @Autowired
    private ImageCacheClient imageCacheClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Image findById(Long id) {
        // 圖片緩存
        return imageCacheClient.queryWithMutexAndLogicExpire(
                CACHE_IMG,
                LOCK_IMG,
                id,
                CACHE_IMG_TTL,
                TimeUnit.SECONDS,
                imageDao::findById
        );
    }

    @Override
    public Image upload(ImageUploadRequest imageUploadRequest) {
        MultipartFile file = imageUploadRequest.getFile();
        String comment = imageUploadRequest.getComment();
        Boolean cacheEnabled = imageUploadRequest.getCacheEnabled();
        Boolean resizeEnabled = imageUploadRequest.getResizeEnabled();
        Integer width = imageUploadRequest.getWidth();
        Integer height = imageUploadRequest.getHeight();

        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("沒有上傳任何檔案");
            }

            String contentType = file.getContentType();
            if (contentType == null) {
                // 阻擋不合規定的contentType
                throw new IllegalArgumentException("檔案的類型異常");
            }
            switch (contentType) {
                case "image/jpeg":
                case "image/png":
                case "image/gif":
                case "image/webp":
                case "image/bmp":
                    break;
                default:
                    throw new IllegalArgumentException("不符合規定的檔案格式");
            }

            if (comment == null || comment.isEmpty()) {
                // 如果無傳遞描述, 則使用原始的檔名作為描述
                comment = file.getOriginalFilename();
            }

            byte[] data = file.getBytes();
            BufferedImage bufferedImage = ImageUtils.getBufferedImage(data);

            // 無設定是否進行圖片壓縮的情形
            if (resizeEnabled == null) {
                if (width != null || height != null) {
                    // 傳入長寬其中一個參數, 視為開啟壓縮
                    resizeEnabled = true;
                } else if (bufferedImage.getWidth() > 1600) {
                    // 寬度超過1600, 進行壓縮
                    width = 1600;
                    resizeEnabled = true;
                } else if (bufferedImage.getHeight() > 900) {
                    // 寬度超過900, 進行壓縮
                    height = 900;
                    resizeEnabled = true;
                } else {
                    resizeEnabled = false;
                }
            } else if (resizeEnabled && width == null && height == null) {
                throw new IllegalArgumentException("要開啟圖片壓縮, 請至少設定寬度或是高度");
            }

            // 進行圖片壓縮
            if (resizeEnabled) {
                data = ImageUtils.resizeImage(bufferedImage, width, height, 0.85f);
                contentType = "image/jpeg";
            }

            Image image = new Image();
            // 從id worker中取得ID
            image.setId(idWorker.nextId("image"));

            // 設定對應參數
            image.setComment(comment);
            image.setMimetype(contentType);
            image.setData(data);

            // 設定圖片緩存機制 (無傳遞參數的處理方式, 檔案小於設定值時啟動緩存)
            if (cacheEnabled == null) {
                cacheEnabled = image.getData().length <= CACHE_IMG_SIZE;
            }
            image.setCacheEnabled(cacheEnabled);

            return image;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Image save(Image image) {
        // 檢查並刪除redis中的資料
        stringRedisTemplate.delete(CACHE_IMG + image.getId());
        return imageDao.save(image);
    }

    @Override
    public void deleteById(Long id) {
        imageDao.deleteById(id);
    }
}
