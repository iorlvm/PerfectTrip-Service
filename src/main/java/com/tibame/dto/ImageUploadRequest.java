package com.tibame.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadRequest {
    private MultipartFile file;
    private String comment;
    private Boolean cacheEnabled;
    private Integer width;
    private Integer height;
}
