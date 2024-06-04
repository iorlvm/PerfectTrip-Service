package com.tibame.example.controller;

import com.tibame.dto.Result;
import com.tibame.example.service.ExampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


// 請使用REST風格
// 查詢相關使用@GetMapping
// 新增相關使用@PostMapping
// 更新相關使用@PutMapping
// 刪除相關使用@DeleteMapping
@RestController
@RequestMapping("/example")
public class ExampleController {
    @Autowired
    private ExampleService exampleService;

    @GetMapping
    public Result getAllUsers() {
        return exampleService.getAll();
    }

    @GetMapping("/{id}")
    public Result getUserById(@PathVariable Long id) {
        return exampleService.getById(id);
    }
}
