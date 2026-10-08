package com.bnppf.bookstore_be.service;

import com.bnppf.bookstore_be.records.RegisterUserRequest;
import com.bnppf.bookstore_be.records.UserResponse;

public interface UserService {

    UserResponse register(RegisterUserRequest request);

    UserResponse getUserByUsername(String username);
}
