package com.bnppf.bookstore_be.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientBookStockException extends RuntimeException {

    public InsufficientBookStockException(Long bookId) {
        super("Requested quantity exceeds available stock for book: " + bookId);
    }
}
