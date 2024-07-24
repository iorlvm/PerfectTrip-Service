package com.tibame.image.controller;

import com.tibame.dto.ImageUploadRequest;
import com.tibame.dto.Result;
import com.tibame.entity.Image;
import com.tibame.image.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/image")
public class ImageController {
    @Autowired
    private ImageService imageService;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImageById(@PathVariable Long id) {
        Image image = imageService.findById(id);
        if (image != null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(image.getMimetype()));
            return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping
    public Result handleFileUpload(ImageUploadRequest imageUploadRequest) {
        try {
            // 將上傳的檔案處理成需要的格式
            Image image = imageService.upload(imageUploadRequest);

            String url = "image/" + imageService.save(image).getId();
            return Result.ok(url);
        }catch (IllegalArgumentException e) {
            // 接收imageService拋出的異常訊息
            return Result.fail("上傳失敗：" + e.getMessage());
        } catch (Exception e) {
            // TODO: 考慮是否要做log處理
            e.printStackTrace();
            return Result.fail("上傳失敗：系統錯誤");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        imageService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
