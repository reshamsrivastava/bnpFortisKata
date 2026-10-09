package com.bnppf.bookstore_be.records.book;

import java.math.BigDecimal;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        String category,
        String description,
        BigDecimal price,
        Integer stock) {
}
