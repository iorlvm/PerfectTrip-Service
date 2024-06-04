package com.tibame.example.service;

import com.tibame.dto.Result;
import com.tibame.entity.ExampleEntity;

import java.util.List;

public interface ExampleService {
    // 小組規定 :service interface定義的method一定要依照這個格式寫註解
    /**
     * 範例與解說：
     * 查詢所有的使用者列表       (服務的功能)
     * @return 所有的user列表   (返回給前端的物件)
     * ※注意：回傳值統一使用Result做包裝
     * 成功回傳：Result.ok(回傳的物件)
     * 失敗回傳：Result.fail("字串-錯誤訊息")
     */
    Result getAll();

    /**
     * 依照指定的id尋找user
     * @param id 搜尋目標的id
     * @return 找到的user
     */
    Result getById(Long id);
}
