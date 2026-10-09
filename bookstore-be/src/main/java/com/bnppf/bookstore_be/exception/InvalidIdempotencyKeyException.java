package com.bnppf.bookstore_be.exception;

import com.bnppf.bookstore_be.constants.OrderConstraints;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super(OrderConstraints.IDEMPOTENCY_KEY_HEADER
                + " must contain between 1 and "
                + OrderConstraints.MAX_IDEMPOTENCY_KEY_LENGTH
                + " non-whitespace characters");
    }
}
