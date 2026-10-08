package com.bnppf.bookstore_be.controller;

import com.bnppf.bookstore_be.records.cart.AddCartItemRequest;
import com.bnppf.bookstore_be.records.cart.CartResponse;
import com.bnppf.bookstore_be.records.cart.UpdateCartItemRequest;
import com.bnppf.bookstore_be.service.cart.CartService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(Principal principal) {
        return cartService.getCart(principal.getName());
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            Principal principal,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addItem(principal.getName(), request));
    }

    @PutMapping("/items/{id}")
    public CartResponse updateItem(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(principal.getName(), id, request);
    }

    @DeleteMapping("/items/{id}")
    public CartResponse removeItem(Principal principal, @PathVariable Long id) {
        return cartService.removeItem(principal.getName(), id);
    }
}
