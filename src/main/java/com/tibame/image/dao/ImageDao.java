package com.tibame.image.dao;

import com.tibame.entity.Image;
import org.springframework.stereotype.Repository;

public interface ImageDao  {
    Image findById(Long id);

    Image save(Image image);
    
    void deleteById(Long id);

}
