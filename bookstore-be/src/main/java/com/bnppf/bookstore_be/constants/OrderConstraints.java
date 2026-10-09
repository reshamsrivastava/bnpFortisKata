package com.bnppf.bookstore_be.constants;

public final class OrderConstraints {

    public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 255;
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private OrderConstraints() {
    }
}
