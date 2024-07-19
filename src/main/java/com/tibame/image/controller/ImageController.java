package com.tibame.image.controller;

import com.tibame.dto.Result;
import com.tibame.entity.Image;
import com.tibame.image.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



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
    public Result handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "comment", required = false, defaultValue = "") String comment,
            @RequestParam(value = "cache", required = false, defaultValue = "auto") String cacheEnabled
    ) {
        if (file.isEmpty()) return Result.fail("沒有上傳任何檔案");

        // 將上傳的檔案處理成需要的格式
        Image image = imageService.upload(file);
        if (image == null) return Result.fail("不符合規定的檔案類型");

        // 將其他參數設定進去
        image.setComment(!comment.isEmpty() ? comment : null);

        // 是否開啟圖片緩存機制 (預設為auto, 檔案大小在50kb以下會開啟圖片緩存)
        if ("y".equals(cacheEnabled)) {
            image.setCacheEnabled(true);
        } else if ("n".equals(cacheEnabled)) {
            image.setCacheEnabled(false);
        }

        imageService.save(image);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        imageService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
