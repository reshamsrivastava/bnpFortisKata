package com.bnppf.bookstore_be.service.cart;

import com.bnppf.bookstore_be.exception.BookNotFoundException;
import com.bnppf.bookstore_be.exception.CartItemNotFoundException;
import com.bnppf.bookstore_be.exception.InsufficientBookStockException;
import com.bnppf.bookstore_be.jpa.entity.BookEntity;
import com.bnppf.bookstore_be.jpa.entity.CartEntity;
import com.bnppf.bookstore_be.jpa.entity.CartItemEntity;
import com.bnppf.bookstore_be.jpa.entity.UserEntity;
import com.bnppf.bookstore_be.jpa.repository.BookRepository;
import com.bnppf.bookstore_be.jpa.repository.CartRepository;
import com.bnppf.bookstore_be.jpa.repository.UserRepository;
import com.bnppf.bookstore_be.records.cart.AddCartItemRequest;
import com.bnppf.bookstore_be.records.cart.CartItemResponse;
import com.bnppf.bookstore_be.records.cart.CartResponse;
import com.bnppf.bookstore_be.records.cart.UpdateCartItemRequest;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Override
    @Transactional
    public CartResponse getCart(String username) {
        return toResponse(getOrCreateCart(username));
    }

    @Override
    @Transactional
    public CartResponse addItem(String username, AddCartItemRequest request) {
        CartEntity cart = getOrCreateCart(username);
        BookEntity book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookNotFoundException(request.bookId()));

        CartItemEntity existingItem = cart.findItemByBookId(book.getId());
        int newQuantity = request.quantity()
                + (existingItem == null ? 0 : existingItem.getQuantity());
        ensureStockAvailable(book, newQuantity);

        if (existingItem == null) {
            cart.addItem(new CartItemEntity(cart, book, request.quantity()));
        } else {
            existingItem.updateQuantity(newQuantity);
        }

        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItem(String username, Long itemId, UpdateCartItemRequest request) {
        CartEntity cart = getOrCreateCart(username);
        CartItemEntity item = findItem(cart, itemId);
        ensureStockAvailable(item.getBook(), request.quantity());
        item.updateQuantity(request.quantity());
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(String username, Long itemId) {
        CartEntity cart = getOrCreateCart(username);
        cart.removeItem(findItem(cart, itemId));
        return toResponse(cartRepository.save(cart));
    }

    private CartEntity getOrCreateCart(String username) {
        return cartRepository.findByUser_Username(username)
                .orElseGet(() -> {
                    UserEntity user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new IllegalStateException(
                                    "Authenticated user was not found: " + username));
                    return cartRepository.save(new CartEntity(user));
                });
    }

    private CartItemEntity findItem(CartEntity cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
    }

    private void ensureStockAvailable(BookEntity book, int quantity) {
        if (quantity > book.getStock()) {
            throw new InsufficientBookStockException(book.getId());
        }
    }

    private CartResponse toResponse(CartEntity cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> new CartItemResponse(
                        item.getId(),
                        item.getBook().getId(),
                        item.getBook().getTitle(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();
        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, total);
    }
}
