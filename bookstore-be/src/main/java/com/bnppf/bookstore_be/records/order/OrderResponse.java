package com.bnppf.bookstore_be.records.order;

import com.bnppf.bookstore_be.jpa.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal total,
        Instant createdAt) {
}
