package com.bnppf.bookstore_be.controller;

import com.bnppf.bookstore_be.records.order.OrderResponse;
import com.bnppf.bookstore_be.service.order.OrderService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            Principal principal,
            @RequestHeader(name = "Idempotency-Key", required = true) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.checkout(principal.getName(), idempotencyKey));
    }
}
