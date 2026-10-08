package com.bnppf.bookstore_be.service.user;

import com.bnppf.bookstore_be.records.user.RegisterUserRequest;
import com.bnppf.bookstore_be.records.user.UserResponse;

public interface UserService {

    UserResponse register(RegisterUserRequest request);

    UserResponse getUserByUsername(String username);
}
