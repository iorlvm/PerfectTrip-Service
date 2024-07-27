package com.tibame.warehouse.dao.impl.impl;

import com.tibame.entity.WarehouseEntity;
import com.tibame.warehouse.dao.impl.WarehouseDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WarehouseDaoImpl implements WarehouseDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Override
    public WarehouseEntity findById(Long WarehouseId) {
        String sql = "select * from warehouse where WarehouseId = ?";
        return jdbcTemplate.queryForObject(sql,
                new BeanPropertyRowMapper<>(WarehouseEntity.class), WarehouseId);
    }

    @Override
    public List<WarehouseEntity> findAll() {
        String sql = "select * from warehouse";
        return jdbcTemplate.query(sql,
                new BeanPropertyRowMapper<>(WarehouseEntity.class));

    }

    @Override
    public void save(WarehouseEntity warehouse) {
        String sql = "INSERT INTO warehouse (productId, stock, location, lastUpdateDate) VALUES (?,?,?,?)";
        jdbcTemplate.update(sql,warehouse.getProductId(),warehouse.getStock(),warehouse.getLocation(),warehouse.getLastUpdateDate());
    }

    @Override
    public void update(WarehouseEntity warehouse) {
        String sql = "update warehouse set productId = ?,stock = ?,location = ?,lastUpdateDate = ? where WarehouseId = ?";
        jdbcTemplate.update(sql,warehouse.getProductId(),warehouse.getStock(),warehouse.getLocation(),warehouse.getLastUpdateDate(),warehouse.getWarehouseId());
    }

    @Override
    public void deleteById(Long WarehouseId) {
        String sql = "delete from warehouse where WarehouseId = ?";
        jdbcTemplate.update(sql,WarehouseId);

    }
}

