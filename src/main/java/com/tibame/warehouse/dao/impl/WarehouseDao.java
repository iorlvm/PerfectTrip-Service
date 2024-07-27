package com.tibame.warehouse.dao.impl;


import com.tibame.entity.WarehouseEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * ClassName:Warehouse
 * Package: com.tibame.warehouse.dao.impl
 * Description:
 *
 * @Author: Jacob
 * @Create: 2024/7/13 - 下午3:46
 * @Version: v1.0
 */
public interface WarehouseDao{

    // 尋找並返回 WarehouseEntity
    WarehouseEntity findById(Long WarehouseId);

    // 返回所有WarehouseEntity的列表形式
    List<WarehouseEntity> findAll();

    // 保存 WarehouseEntity 不返回任何值
    void save(WarehouseEntity warehouse);

    // 更新 WarehouseEntity 不返回任何值
    void update(WarehouseEntity warehouse);

    // 刪除 WarehouseEntity 不返回任何值
    void deleteById(Long WarehouseId);


}




