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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Override
    @Transactional
    public CartResponse getCart(String username) {
        validateUsername(username);
        return toResponse(getOrCreateCart(username));
    }

    @Override
    @Transactional
    public CartResponse addItem(String username, AddCartItemRequest request) {
        validateUsername(username);
        if (request == null || request.bookId() == null || request.bookId() < 1) {
            throw new IllegalArgumentException("Cart item request and book id are required");
        }
        validateQuantity(request.quantity());
        CartEntity cart = getOrCreateCartForUpdate(username);
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

        CartEntity savedCart = cartRepository.save(cart);
        log.atInfo()
                .addKeyValue("event", "cart.item.added")
                .addKeyValue("username", username)
                .addKeyValue("bookId", book.getId())
                .addKeyValue("quantity", newQuantity)
                .log("Item added to cart");
        return toResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse updateItem(String username, Long itemId, UpdateCartItemRequest request) {
        validateUsername(username);
        if (itemId == null || request == null) {
            throw new IllegalArgumentException("Cart item id and update request are required");
        }
        validateQuantity(request.quantity());
        CartEntity cart = getOrCreateCartForUpdate(username);
        CartItemEntity item = findItem(cart, itemId);
        ensureStockAvailable(item.getBook(), request.quantity());
        item.updateQuantity(request.quantity());
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(String username, Long itemId) {
        validateUsername(username);
        if (itemId == null) {
            throw new IllegalArgumentException("Cart item id must not be null");
        }
        CartEntity cart = getOrCreateCartForUpdate(username);
        cart.removeItem(findItem(cart, itemId));
        return toResponse(cartRepository.save(cart));
    }

    private CartEntity getOrCreateCart(String username) {
        return cartRepository.findByUser_Username(username)
                .orElseGet(() -> createCartAfterLockingUser(username, false));
    }

    private CartEntity getOrCreateCartForUpdate(String username) {
        return cartRepository.findByUser_UsernameForUpdate(username)
                .orElseGet(() -> createCartAfterLockingUser(username, true));
    }

    private CartEntity createCartAfterLockingUser(String username, boolean lockCart) {
        UserEntity user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user was not found: " + username));
        return (lockCart
                ? cartRepository.findByUser_UsernameForUpdate(username)
                : cartRepository.findByUser_Username(username))
                .orElseGet(() -> cartRepository.save(new CartEntity(user)));
    }

    private CartItemEntity findItem(CartEntity cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
    }

    private void ensureStockAvailable(BookEntity book, int quantity) {
        if (quantity > book.getStock()) {
            throw new InsufficientBookStockException(book.getId());
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least one");
        }
    }

    private CartResponse toResponse(CartEntity cart) {
        List<CartItemResponse> items = cartRepository.findItemResponsesByCartId(cart.getId());
        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, total);
    }
}
