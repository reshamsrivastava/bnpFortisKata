package com.bnppf.bookstore_be.records.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        Long bookId,
        String title,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {
}
