package com.tibame.example.dao;

import com.tibame.entity.ExampleEntity;

import java.util.List;

public interface ExampleDao {
    ExampleEntity findById(Long id);

    List<ExampleEntity> findAll();

    ExampleEntity findByPhone(String phone);

    public boolean create(ExampleEntity exampleEntity);

    boolean deleteById(Long id);

    boolean update(ExampleEntity exampleEntity);
}
