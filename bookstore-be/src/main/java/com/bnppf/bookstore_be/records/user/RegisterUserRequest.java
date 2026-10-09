package com.bnppf.bookstore_be.records.user;

import com.bnppf.bookstore_be.constants.UserConstraints;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank @Size(
                min = UserConstraints.MIN_USERNAME_LENGTH,
                max = UserConstraints.MAX_USERNAME_LENGTH) String username,
        @NotBlank @Email @Size(max = UserConstraints.MAX_EMAIL_LENGTH) String email,
        @NotBlank @Size(
                min = UserConstraints.MIN_PASSWORD_LENGTH,
                max = UserConstraints.MAX_PASSWORD_LENGTH) String password) {
}
