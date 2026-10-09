package com.bnppf.bookstore_be.records.order;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long bookId,
        String title,
        String author,
        String isbn,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {
}
