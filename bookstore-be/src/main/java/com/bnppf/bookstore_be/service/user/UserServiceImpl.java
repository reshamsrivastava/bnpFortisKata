package com.bnppf.bookstore_be.service.user;

import com.bnppf.bookstore_be.records.user.RegisterUserRequest;
import com.bnppf.bookstore_be.records.user.UserResponse;
import com.bnppf.bookstore_be.jpa.entity.UserEntity;
import com.bnppf.bookstore_be.jpa.repository.UserRepository;
import com.bnppf.bookstore_be.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request must not be null");
        }
        if (userRepository.existsByUsername(request.username())
                || userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException();
        }

        UserEntity user = new UserEntity(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()));
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
        return toResponse(user);
    }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
