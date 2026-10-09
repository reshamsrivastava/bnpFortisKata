package com.bnppf.bookstore_be.service.order;

import com.bnppf.bookstore_be.records.order.OrderResponse;

public interface OrderService {

    OrderResponse checkout(String username, String idempotencyKey);
}
