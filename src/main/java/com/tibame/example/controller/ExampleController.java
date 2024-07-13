package com.tibame.example.controller;

import com.tibame.dto.Result;
import com.tibame.entity.ExampleEntity;
import com.tibame.example.service.ExampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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

    // Controller的回傳值使用Result物件做包裝，方便前端的錯誤提示
    @GetMapping
    public Result getAllUsers() {
        List<ExampleEntity> allUsers = exampleService.getAll();

        // 避免程式碼邏輯嵌套，適時使用反向邏輯的撰寫風格    ※不強制但建議
        if (allUsers == null || allUsers.isEmpty()) {
            // 依照邏輯這個東西不太可能搜不到資料，但這是範例
            return Result.fail("出事啦！用戶資料被人全刪掉了啦！");
        }

        // 回傳值是List的情況 (前端有資料總筆數比較好計算分頁切換)
        return Result.ok(allUsers, (long) allUsers.size());
    }

    @GetMapping("/{id}")
    public Result getUserById(@PathVariable Long id) {
        ExampleEntity user = exampleService.getById(id);
        if (user == null) {
            // 錯誤的時候夾帶錯誤訊息回復前端
            return Result.fail("找不到該用戶");
        } else {
            // 回傳值是單一物件的情況
            return Result.ok(user);
        }
    }

    @PostMapping
    public Result createUser(@RequestBody ExampleEntity user) {
        if (user == null || StringUtils.isEmpty(user.getPhone())) {
            return Result.fail("user或是phone不得為空");
        }

        if (exampleService.create(user)) {
            return Result.ok(user);
        }else {
            return Result.fail("新增失敗, 請檢查傳入的數值格式是否正確");
        }
    }

    @DeleteMapping("/{id}")
    public Result deleteUserById(@PathVariable Long id) {
        if (id == null) {
            return Result.fail("ID不得為空");
        }

        if (exampleService.deleteById(id)) {
            return Result.ok();
        } else {
            return Result.fail("刪除失敗, 請檢查傳入的數值是否正確");
        }
    }

    @PutMapping
    public Result updateUser(@RequestBody ExampleEntity user) {
        if (user == null || user.getId() == null) {
            return Result.fail("用戶或用戶ID不得為空");
        }

        if (exampleService.update(user) != null) {
            return Result.ok();
        } else {
            return Result.fail("更新失敗, 請檢查傳入的數值格式是否正確");
        }
    }
}
