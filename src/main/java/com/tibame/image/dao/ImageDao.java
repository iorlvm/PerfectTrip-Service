package com.tibame.image.dao;

import com.tibame.entity.Image;

public interface ImageDao  {
    Image findById(Long id);

    Image save(Image image);

    void deleteById(Long id);

}
