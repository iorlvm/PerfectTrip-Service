package com.tibame.entity;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Data  // 自動生成getter setter toString
@Entity
@Table(name = "tb_example")
public class ExampleEntity implements Serializable {

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

    @Column(name = "create_time", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp updateTime;
}
