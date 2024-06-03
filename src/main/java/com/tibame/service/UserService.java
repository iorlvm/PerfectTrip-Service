package com.tibame.service;

import com.tibame.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> getAllUsers();

    User getUserById(Long id);
}
