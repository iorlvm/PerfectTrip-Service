package com.tibame.warehouse.service.impl;

import com.tibame.entity.WarehouseEntity;
import com.tibame.warehouse.dao.impl.WarehouseDao;
import com.tibame.warehouse.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WarehouseServiceImpl implements WarehouseService {

    @Autowired
    private WarehouseDao warehouseDao;

    @Override
    public WarehouseEntity warehouse(WarehouseEntity warehouse) {
        return null;
    }

    @Override
    public WarehouseEntity getWarehouseById(Long id) {
        return warehouseDao.findById(id);

    }

    @Override
    public WarehouseEntity createWarehouse(WarehouseEntity warehouse) {
        warehouseDao.save(warehouse);
        return warehouse;
    }

    @Override
    public WarehouseEntity updateWarehouse(Long id, WarehouseEntity warehouseDetails) {
        WarehouseEntity warehouse = warehouseDao.findById(id);
        if(warehouse != null) {
            warehouse.setWarehouseId(warehouseDetails.getWarehouseId());
            warehouse.setProductId(warehouseDetails.getProductId());
            warehouse.setStock(warehouseDetails.getStock());
            warehouse.setLocation(warehouseDetails.getLocation());
            warehouse.setLastUpdateDate(warehouseDetails.getLastUpdateDate());
            warehouseDao.update(warehouse);

        }
        return warehouse;
    }

    @Override
    public Boolean deleteWarehouse(Long id) {
        WarehouseEntity warehouse = warehouseDao.findById(id);
        if(warehouse != null) {
            warehouseDao.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<WarehouseEntity> getAllWarehouses() {
        List<WarehouseEntity> all = warehouseDao.findAll();
        for(WarehouseEntity warehouse : all) {
            System.out.println(warehouse);
        }
        return all;
    }
}
