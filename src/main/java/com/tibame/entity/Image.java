package com.tibame.entity;

import lombok.Data;
import lombok.Getter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@Entity
@Table(name = "images")
public class Image implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false) // 使用自定義的RedisIdWorker
    private Long id;

    @Lob
    @Column(name = "data")
    private byte[] data;

    @Column(name = "comment", length = 255)
    private String comment;

    @Column(name = "mimetype", nullable = false)
    private String mimetype;

    @Column(name = "cache_enabled", nullable = false)
    private boolean cacheEnabled = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private Date createdAt;
}
