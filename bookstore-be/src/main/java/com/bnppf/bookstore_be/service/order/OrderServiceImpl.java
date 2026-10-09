package com.bnppf.bookstore_be.service.order;

import com.bnppf.bookstore_be.exception.EmptyCartException;
import com.bnppf.bookstore_be.exception.InsufficientBookStockException;
import com.bnppf.bookstore_be.exception.InvalidIdempotencyKeyException;
import com.bnppf.bookstore_be.jpa.entity.BookEntity;
import com.bnppf.bookstore_be.jpa.entity.CartEntity;
import com.bnppf.bookstore_be.jpa.entity.CartItemEntity;
import com.bnppf.bookstore_be.jpa.entity.OrderEntity;
import com.bnppf.bookstore_be.jpa.entity.OrderItemEntity;
import com.bnppf.bookstore_be.jpa.repository.BookRepository;
import com.bnppf.bookstore_be.jpa.repository.CartRepository;
import com.bnppf.bookstore_be.jpa.repository.OrderRepository;
import com.bnppf.bookstore_be.records.order.OrderItemResponse;
import com.bnppf.bookstore_be.records.order.OrderResponse;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse checkout(String username, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);

        CartEntity cart = cartRepository.findByUser_UsernameForUpdate(username)
                .orElseThrow(EmptyCartException::new);

        var existingOrder = orderRepository
                .findByUser_UsernameAndIdempotencyKey(username, idempotencyKey);
        if (existingOrder.isPresent()) {
            return toResponse(existingOrder.get());
        }

        List<CartItemEntity> cartItems = cart.getItems().stream()
                .sorted((first, second) -> first.getBook().getId()
                        .compareTo(second.getBook().getId()))
                .toList();
        if (cartItems.isEmpty()) {
            throw new EmptyCartException();
        }

        List<Long> bookIds = cartItems.stream()
                .map(item -> item.getBook().getId())
                .toList();
        Map<Long, BookEntity> booksById = bookRepository.findAllByIdInForUpdate(bookIds).stream()
                .collect(Collectors.toMap(BookEntity::getId, Function.identity()));

        OrderEntity order = new OrderEntity(cart.getUser(), idempotencyKey);
        for (CartItemEntity cartItem : cartItems) {
            Long bookId = cartItem.getBook().getId();
            BookEntity book = booksById.get(bookId);
            if (book == null) {
                throw new IllegalStateException(
                        "Cart references a book that no longer exists: " + bookId);
            }
            if (cartItem.getQuantity() > book.getStock()) {
                throw new InsufficientBookStockException(book.getId());
            }

            book.reduceStock(cartItem.getQuantity());
            order.addItem(new OrderItemEntity(order, cartItem, book));
        }
        order.complete();
        OrderEntity savedOrder = orderRepository.save(order);
        cart.clearItems();
        return toResponse(savedOrder);
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 255) {
            throw new InvalidIdempotencyKeyException();
        }
    }

    private OrderResponse toResponse(OrderEntity order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getBookId(),
                        item.getTitle(),
                        item.getAuthor(),
                        item.getIsbn(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                items,
                order.getTotal(),
                order.getCreatedAt());
    }
}
