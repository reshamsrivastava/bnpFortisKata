package com.bnppf.bookstore_be.service.cart;

import com.bnppf.bookstore_be.records.cart.AddCartItemRequest;
import com.bnppf.bookstore_be.records.cart.CartResponse;
import com.bnppf.bookstore_be.records.cart.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCart(String username);

    CartResponse addItem(String username, AddCartItemRequest request);

    CartResponse updateItem(String username, Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(String username, Long itemId);
}
