package com.tibame.example.repository;

import com.tibame.entity.ExampleEntity;

import java.util.List;

public interface ExampleDao {
    ExampleEntity findById(Long id);

    List<ExampleEntity> findAll();
}
