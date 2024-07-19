package com.tibame.image.service;

import com.tibame.dto.ImageUploadRequest;
import com.tibame.entity.Image;

public interface ImageService {

    /**
     * 根據id查詢圖片
     * @param id 圖片的id
     * @return 查詢到的圖片
     */
    Image findById(Long id);

    /**
     * 將上傳的檔案處理成Image物件
     * 並使用id worker取得id (尚未儲存進資料庫)
     * @param imageUploadRequest 前端上傳的檔案以及相關設定
     * @return 處理後的Image物件
     */
    Image upload(ImageUploadRequest imageUploadRequest);

    /**
     * 將Image物件存入資料庫
     * @param image 處理完畢的Image物件
     * @return Image物件
     */
    Image save(Image image);

    /**
     * 根據id刪除對應的照片
     * @param id 圖片的id
     */
    void deleteById(Long id);
}
