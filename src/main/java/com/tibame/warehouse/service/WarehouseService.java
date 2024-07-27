package com.tibame.warehouse.service;

import com.tibame.entity.WarehouseEntity;

import java.util.List;

/**
 * ClassName:Warehouse
 * Package: com.tibame.warehouse.service
 * Description:
 *
 * @Author: Jacob
 * @Create: 2024/7/13 - 下午3:49
 * @Version: v1.0
 */
public interface WarehouseService{

    WarehouseEntity warehouse(WarehouseEntity warehouse);
    WarehouseEntity getWarehouseById(Long id);
    WarehouseEntity createWarehouse(WarehouseEntity warehouse);
    WarehouseEntity updateWarehouse(Long id, WarehouseEntity warehouseDetails);
    Boolean deleteWarehouse(Long id);
    List<WarehouseEntity> getAllWarehouses();
}
