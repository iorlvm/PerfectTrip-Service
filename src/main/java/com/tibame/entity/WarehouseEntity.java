package com.tibame.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse")
public class WarehouseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long warehouseId;

    private Long productId;

    private int stock;

    @Column(name = "location")
    private String location;

    private LocalDateTime lastUpdateDate;

    public WarehouseEntity(Long warehouseId, Long productId, int stock, String location, LocalDateTime lastUpdateDate) {
        this.warehouseId = warehouseId;
        this.productId = productId;
        this.stock = stock;
        this.location = location;
        this.lastUpdateDate = lastUpdateDate;
    }

    public WarehouseEntity() {

    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(LocalDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    @Override
    public String toString() {
        return "WarehouseEntity{" +
                "warehouseId=" + warehouseId +
                ", productId=" + productId +
                ", stock=" + stock +
                ", location='" + location + '\'' +
                ", lastUpdateDate=" + lastUpdateDate +
                '}';
    }
}
