package com.tibame.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Data  // 自動生成getter setter toString
@Entity
@Table(name = "tb_example")
public class ExampleEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "password")
    private String password;

    @Column(name = "nick_name")
    private String nickName;

    @Column(name = "icon")
    private String icon;

    @Transient   // 瞬時的, 設定這個註解的欄位不會被寫入資料庫 (舉例用的欄位, 目前沒有實際用途)
    private String temporaryData;

    @Column(name = "create_time",
            nullable = false,     // 不可為空
            insertable = false,   // 新增時無視輸入的值   在這個情況下是給資料庫生成時間
            updatable = false,    // 修改時無視輸入的值   在這個情況下是這個資料在創建後就不可被修改
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" // 設置SQL中的類型與預設值
    )
    private Timestamp createTime;

    @Column(name = "update_time",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
    )
    private Timestamp updateTime;
}
