package com.tibame.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data                   // 自動生成getter setter toString
@NoArgsConstructor      // 自動生成無參數建構子
@AllArgsConstructor     // 自動生成全部參數的建構子
public class Result {
    private Boolean success;
    private String errorMsg;
    private Object data;
    private Long total;

    public static Result ok() {
        return new Result(true, null, null, null);
    }

    public static Result ok(Object data) {
        return new Result(true, null, data, null);
    }

    public static Result ok(List<?> data, Long total) {
        return new Result(true, null, data, total);
    }

    public static Result fail(String errorMsg) {
        return new Result(false, errorMsg, null, null);
    }
}
