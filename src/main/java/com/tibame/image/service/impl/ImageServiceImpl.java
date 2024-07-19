package com.tibame.image.service.impl;

import com.tibame.dto.ImageUploadRequest;
import com.tibame.entity.Image;
import com.tibame.image.dao.ImageDao;
import com.tibame.image.service.ImageService;
import com.tibame.utils.basic.ImageUtils;
import com.tibame.utils.redis.RedisIdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

import static com.tibame.utils.redis.RedisConstants.CACHE_IMG_SIZE;

@Service
@Transactional
public class ImageServiceImpl implements ImageService {
    @Autowired
    private ImageDao imageDao;
    @Autowired
    private RedisIdWorker idWorker;

    @Override
    public Image findById(Long id) {
        // TODO: 圖片緩存
        return imageDao.findById(id);
    }

    @Override
    public Image upload(ImageUploadRequest imageUploadRequest) {
        MultipartFile file = imageUploadRequest.getFile();
        String comment = imageUploadRequest.getComment();
        Boolean cacheEnabled = imageUploadRequest.getCacheEnabled();
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
                // 如果無傳遞備註, 則使用原始的檔名作為備註
                comment = file.getOriginalFilename();
            }

            byte[] data = file.getBytes();
            BufferedImage bufferedImage = ImageUtils.getBufferedImage(data);

            // 圖片縮放參數
            if (width == null && height == null) {
                // 長寬皆無設定的情形, 寬度超過1600 高度超過900才做處裡
                width = Math.min(bufferedImage.getWidth(), 1600);
                height = Math.min(bufferedImage.getHeight(), 900);
            }

            // 進行圖片壓縮
            data = ImageUtils.resizeImage(bufferedImage, width, height, 0.85f);

            Image image = new Image();
            // 從id worker中取得ID
            image.setId(idWorker.nextId("image"));

            // 設定對應參數
            image.setComment(comment);
            image.setMimetype("image/jpeg");
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
        return imageDao.save(image);
    }

    @Override
    public void deleteById(Long id) {
        imageDao.deleteById(id);
    }
}
