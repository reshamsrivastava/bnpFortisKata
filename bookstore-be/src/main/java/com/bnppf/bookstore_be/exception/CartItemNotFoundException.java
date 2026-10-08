package com.bnppf.bookstore_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long itemId) {
        super("Cart item not found: " + itemId);
    }
}
