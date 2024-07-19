package com.tibame.image.service;

import com.tibame.entity.Image;
import org.springframework.web.multipart.MultipartFile;

public interface ImageService {

    Image findById(Long id);

    Image upload(MultipartFile file);

    Image save(Image image);

    void deleteById(Long id);
}
