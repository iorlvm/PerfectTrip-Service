package com.tibame.image.service.impl;

import com.tibame.entity.Image;
import com.tibame.image.dao.ImageDao;
import com.tibame.image.service.ImageService;
import com.tibame.utils.redis.RedisIdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Transactional
public class ImageServiceImpl implements ImageService {
    @Autowired
    private ImageDao imageDao;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Image findById(Long id) {
        return imageDao.findById(id);
    }

    @Override
    public Image upload(MultipartFile file) {
        try {
            String contentType = file.getContentType();
            // TODO: 阻擋不合法的contentType
            byte[] data = file.getBytes();
            Image image = new Image();

            RedisIdWorker idWorker = new RedisIdWorker(stringRedisTemplate);
            image.setId(idWorker.nextId("image"));
            image.setMimetype(contentType);
            image.setData(data);

            // TODO: 根據檔案大小決定是否啟用圖片緩存

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
