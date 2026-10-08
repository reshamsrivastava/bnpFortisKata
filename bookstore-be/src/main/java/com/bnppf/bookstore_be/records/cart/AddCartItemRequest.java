package com.bnppf.bookstore_be.records.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(
        @NotNull @Min(1) Long bookId,
        @NotNull @Min(1) Integer quantity) {
}
