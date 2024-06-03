package com.tibame.repository;

import com.tibame.entity.User;

import java.util.List;

public interface UserDao {
    User findById(Long id);

    List<User> findAll();
}
