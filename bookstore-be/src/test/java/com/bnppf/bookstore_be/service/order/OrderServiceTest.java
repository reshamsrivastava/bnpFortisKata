package com.bnppf.bookstore_be.service.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bnppf.bookstore_be.constants.OrderConstraints;
import com.bnppf.bookstore_be.exception.EmptyCartException;
import com.bnppf.bookstore_be.exception.InsufficientBookStockException;
import com.bnppf.bookstore_be.exception.InvalidIdempotencyKeyException;
import com.bnppf.bookstore_be.jpa.entity.BookEntity;
import com.bnppf.bookstore_be.jpa.entity.CartEntity;
import com.bnppf.bookstore_be.jpa.entity.CartItemEntity;
import com.bnppf.bookstore_be.jpa.entity.OrderEntity;
import com.bnppf.bookstore_be.jpa.entity.OrderItemEntity;
import com.bnppf.bookstore_be.jpa.entity.OrderStatus;
import com.bnppf.bookstore_be.jpa.entity.UserEntity;
import com.bnppf.bookstore_be.jpa.repository.BookRepository;
import com.bnppf.bookstore_be.jpa.repository.CartRepository;
import com.bnppf.bookstore_be.jpa.repository.OrderRepository;
import com.bnppf.bookstore_be.records.order.OrderResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String USERNAME = "reader";
    private static final String KEY = "checkout-123";

    @Mock
    private CartRepository cartRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UserEntity user;
    private CartEntity cart;
    private BookEntity firstBook;
    private BookEntity secondBook;

    @BeforeEach
    void setUp() {
        user = new UserEntity(USERNAME, "reader@example.com", "encoded");
        cart = new CartEntity(user);
        firstBook = book(11L, "First Book", 3, "15.00");
        secondBook = book(22L, "Second Book", 5, "9.00");
    }

    @Test
    void shouldRejectNullOrBlankUsernameBeforeRepositoryAccess() {
        assertThrows(IllegalArgumentException.class, () -> orderService.checkout(null, KEY));
        assertThrows(IllegalArgumentException.class, () -> orderService.checkout(" ", KEY));
        verifyNoInteractions(cartRepository, bookRepository, orderRepository);
    }

    @Test
    void shouldRejectMissingBlankAndOversizedIdempotencyKeys() {
        assertThrows(
                InvalidIdempotencyKeyException.class,
                () -> orderService.checkout(USERNAME, null));
        assertThrows(
                InvalidIdempotencyKeyException.class,
                () -> orderService.checkout(USERNAME, "  "));
        String tooLongKey = "k".repeat(OrderConstraints.MAX_IDEMPOTENCY_KEY_LENGTH + 1);
        assertThrows(
                InvalidIdempotencyKeyException.class,
                () -> orderService.checkout(USERNAME, tooLongKey));
        verifyNoInteractions(cartRepository, bookRepository, orderRepository);
    }

    @Test
    void shouldAcceptMaximumLengthKeyAndReportMissingCart() {
        String maximumLengthKey = "k".repeat(OrderConstraints.MAX_IDEMPOTENCY_KEY_LENGTH);
        when(cartRepository.findByUser_UsernameForUpdate(USERNAME)).thenReturn(Optional.empty());

        assertThrows(
                EmptyCartException.class,
                () -> orderService.checkout(USERNAME, maximumLengthKey));

        verify(cartRepository).findByUser_UsernameForUpdate(USERNAME);
        verifyNoInteractions(bookRepository, orderRepository);
    }

    @Test
    void shouldReturnExistingOrderOnIdempotentRetryWithoutLockingInventory() {
        CartItemEntity priorItem = new CartItemEntity(cart, firstBook, 2);
        OrderEntity existingOrder = new OrderEntity(user, KEY);
        existingOrder.addItem(new OrderItemEntity(
                existingOrder, firstBook, priorItem.getQuantity(), new BigDecimal("12.00")));
        existingOrder.complete();
        ReflectionTestUtils.setField(existingOrder, "id", 91L);
        when(cartRepository.findByUser_UsernameForUpdate(USERNAME))
                .thenReturn(Optional.of(cart));
        when(orderRepository.findByUser_UsernameAndIdempotencyKey(USERNAME, KEY))
                .thenReturn(Optional.of(existingOrder));

        OrderResponse response = orderService.checkout(USERNAME, KEY);

        assertEquals(91L, response.id());
        assertEquals(OrderStatus.COMPLETED, response.status());
        assertEquals(new BigDecimal("24.00"), response.total());
        assertEquals("First Book", response.items().get(0).title());
        assertEquals(new BigDecimal("12.00"), response.items().get(0).unitPrice());
        verifyNoInteractions(bookRepository);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void shouldRejectEmptyCartWhenNoPriorOrderExists() {
        when(cartRepository.findByUser_UsernameForUpdate(USERNAME))
                .thenReturn(Optional.of(cart));
        when(orderRepository.findByUser_UsernameAndIdempotencyKey(USERNAME, KEY))
                .thenReturn(Optional.empty());

        assertThrows(EmptyCartException.class, () -> orderService.checkout(USERNAME, KEY));
        verifyNoInteractions(bookRepository);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void shouldFailWhenLockedBookCannotBeFound() {
        cart.addItem(new CartItemEntity(cart, firstBook, 1));
        when(cartRepository.findByUser_UsernameForUpdate(USERNAME))
                .thenReturn(Optional.of(cart));
        when(orderRepository.findByUser_UsernameAndIdempotencyKey(USERNAME, KEY))
                .thenReturn(Optional.empty());
        when(bookRepository.findAllByIdInForUpdate(List.of(11L))).thenReturn(List.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> orderService.checkout(USERNAME, KEY));

        assertEquals("Cart references a book that no longer exists: 11", exception.getMessage());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void shouldRejectCheckoutWhenLockedStockIsInsufficientAndKeepCartIntact() {
        cart.addItem(new CartItemEntity(cart, firstBook, 4));
        when(cartRepository.findByUser_UsernameForUpdate(USERNAME))
                .thenReturn(Optional.of(cart));
        when(orderRepository.findByUser_UsernameAndIdempotencyKey(USERNAME, KEY))
                .thenReturn(Optional.empty());
        when(bookRepository.findAllByIdInForUpdate(List.of(11L)))
                .thenReturn(List.of(firstBook));

        assertThrows(InsufficientBookStockException.class,
                () -> orderService.checkout(USERNAME, KEY));

        assertEquals(3, firstBook.getStock());
        assertEquals(1, cart.getItems().size());
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void shouldLockBooksInSortedOrderReduceExactStockAndPersistSnapshotTotals() {
        CartItemEntity secondCartItem = new CartItemEntity(cart, secondBook, 2);
        CartItemEntity firstCartItem = new CartItemEntity(cart, firstBook, 3);
        cart.addItem(secondCartItem);
        cart.addItem(firstCartItem);
        when(cartRepository.findByUser_UsernameForUpdate(USERNAME))
                .thenReturn(Optional.of(cart));
        when(orderRepository.findByUser_UsernameAndIdempotencyKey(USERNAME, KEY))
                .thenReturn(Optional.empty());
        when(bookRepository.findAllByIdInForUpdate(List.of(11L, 22L)))
                .thenReturn(List.of(firstBook, secondBook));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 92L);
            return order;
        });

        OrderResponse response = orderService.checkout(USERNAME, KEY);

        assertEquals(92L, response.id());
        assertEquals(OrderStatus.COMPLETED, response.status());
        assertEquals(new BigDecimal("63.00"), response.total());
        assertEquals(2, response.items().size());
        assertEquals("First Book", response.items().get(0).title());
        assertEquals("Second Book", response.items().get(1).title());
        assertEquals(0, firstBook.getStock());
        assertEquals(3, secondBook.getStock());
        assertEquals(List.of(), cart.getItems());

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(new BigDecimal("63.00"), orderCaptor.getValue().getTotal());
        assertEquals(OrderStatus.COMPLETED, orderCaptor.getValue().getStatus());
        verify(bookRepository).findAllByIdInForUpdate(List.of(11L, 22L));
    }

    @Test
    void shouldRejectStockChangeForZeroQuantityThroughDomainEntity() {
        assertThrows(InsufficientBookStockException.class, () -> firstBook.reduceStock(0));
        assertEquals(3, firstBook.getStock());
    }

    private BookEntity book(Long id, String title, int stock, String price) {
        BookEntity book = new BookEntity(
                title,
                "Test Author",
                "97800000000" + id,
                "Testing",
                "Test description",
                new BigDecimal(price),
                stock);
        book.setId(id);
        return book;
    }
}
