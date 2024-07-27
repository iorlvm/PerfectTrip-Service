package com.tibame.warehouse.controller;

import com.tibame.entity.WarehouseEntity;
import com.tibame.warehouse.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/example")
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    @GetMapping{"/{id}"}
    public ResponseEntity<WarehouseEntity> getWarehouseById(@PathVariable Long id) {
        WarehouseEntity warehouse = warehouseService.getWarehouseById(id);
        if(warehouse != null) {
            return new ResponseEntity<>(warehouse, HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

    }

    @PostMapping("/")
    public ResponseEntity<WarehouseEntity> createWarehouse(WarehouseEntity warehouse){
        WarehouseEntity createdWarehouse = warehouseService.createWarehouse(warehouse);
        return new ResponseEntity<>(createdWarehouse,HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseEntity> updateWarehouse(@RequestBody WarehouseEntity warehouse,
                                                           @PathVariable Long id){
        WarehouseEntity warehouseEntity = warehouseService.updateWarehouse(id, warehouse);
        if(warehouseEntity != null) {
            return new ResponseEntity<>(warehouseEntity, HttpStatus.OK);
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WarehouseEntity> deleteWarehouseById(@PathVariable Long id){
        Boolean isDeleted = warehouseService.deleteWarehouse(id);
        if(isDeleted) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
