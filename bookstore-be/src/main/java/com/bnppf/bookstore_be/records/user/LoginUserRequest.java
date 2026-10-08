package com.bnppf.bookstore_be.records.user;

import jakarta.validation.constraints.NotBlank;

public record LoginUserRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
