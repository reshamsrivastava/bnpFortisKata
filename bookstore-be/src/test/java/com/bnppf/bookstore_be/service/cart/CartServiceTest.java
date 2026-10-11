package com.bnppf.bookstore_be.service.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String USERNAME = "reader";

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private UserEntity user;
    private CartEntity cart;
    private BookEntity book;

    @BeforeEach
    void setUp() {
        user = new UserEntity(USERNAME, "reader@example.com", "encoded");
        cart = new CartEntity(user);
        ReflectionTestUtils.setField(cart, "id", 20L);
        book = book(30L, 6);
        lenient().when(cartRepository.save(any(CartEntity.class))).thenAnswer(invocation -> {
            CartEntity savedCart = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedCart, "id", 20L);
            return savedCart;
        });
        lenient().when(cartRepository.findItemResponsesByCartId(20L)).thenReturn(List.of());
    }

    @Test
    void shouldReturnExistingCartProjectionAndZeroTotalForEmptyCart() {
        when(cartRepository.findByUser_Username(USERNAME)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(USERNAME);

        assertEquals(20L, response.id());
        assertEquals(List.of(), response.items());
        assertEquals(BigDecimal.ZERO, response.total());
        verify(cartRepository).findItemResponsesByCartId(20L);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldCreateCartWhenUserHasNone() {
        when(cartRepository.findByUser_Username(USERNAME))
                .thenReturn(Optional.empty(), Optional.empty());
        when(userRepository.findByUsernameForUpdate(USERNAME)).thenReturn(Optional.of(user));

        CartResponse response = cartService.getCart(USERNAME);

        assertEquals(20L, response.id());
        verify(userRepository).findByUsernameForUpdate(USERNAME);
        verify(cartRepository).save(any(CartEntity.class));
    }

    @Test
    void shouldUseCartCreatedConcurrentlyAfterLockingUserForRead() {
        when(cartRepository.findByUser_Username(USERNAME))
                .thenReturn(Optional.empty(), Optional.of(cart));
        when(userRepository.findByUsernameForUpdate(USERNAME)).thenReturn(Optional.of(user));

        CartResponse response = cartService.getCart(USERNAME);

        assertEquals(20L, response.id());
        verify(cartRepository, never()).save(any(CartEntity.class));
    }

    @Test
    void shouldRejectCartCreationForUnknownUser() {
        when(cartRepository.findByUser_Username(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByUsernameForUpdate(USERNAME)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> cartService.getCart(USERNAME));
        verify(cartRepository, never()).save(any(CartEntity.class));
    }

    @Test
    void shouldRejectBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> cartService.getCart(" "));
        verifyNoInteractions(cartRepository, userRepository, bookRepository);
    }

    @Test
    void shouldRejectNullUsername() {
        assertThrows(IllegalArgumentException.class, () -> cartService.getCart(null));
        verifyNoInteractions(cartRepository, userRepository, bookRepository);
    }

    @Test
    void shouldAddNewBookToExistingCart() {
        useLockedCart();
        when(bookRepository.findById(30L)).thenReturn(Optional.of(book));
        when(cartRepository.findItemResponsesByCartId(20L)).thenReturn(
                List.of(itemResponse(40L, 30L, 2, "12.50")));

        CartResponse response = cartService.addItem(USERNAME, new AddCartItemRequest(30L, 2));

        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("25.00"), response.total());
        verify(cartRepository).save(cart);
    }

    @Test
    void shouldIncreaseQuantityWhenBookAlreadyExistsInCart() {
        useLockedCart();
        CartItemEntity existingItem = new CartItemEntity(cart, book, 2);
        cart.addItem(existingItem);
        when(bookRepository.findById(30L)).thenReturn(Optional.of(book));

        cartService.addItem(USERNAME, new AddCartItemRequest(30L, 4));

        assertEquals(6, existingItem.getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void shouldRejectAddWhenBookDoesNotExist() {
        useLockedCart();
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(
                BookNotFoundException.class,
                () -> cartService.addItem(USERNAME, new AddCartItemRequest(999L, 1)));
        verify(cartRepository, never()).save(any(CartEntity.class));
    }

    @Test
    void shouldRejectAddThatExceedsAvailableStock() {
        useLockedCart();
        when(bookRepository.findById(30L)).thenReturn(Optional.of(book));

        assertThrows(
                InsufficientBookStockException.class,
                () -> cartService.addItem(USERNAME, new AddCartItemRequest(30L, 7)));
        verify(cartRepository, never()).save(any(CartEntity.class));
    }

    @Test
    void shouldCreateAndLockCartBeforeFirstAdd() {
        when(cartRepository.findByUser_UsernameForUpdate(USERNAME))
                .thenReturn(Optional.empty(), Optional.empty());
        when(userRepository.findByUsernameForUpdate(USERNAME)).thenReturn(Optional.of(user));
        when(bookRepository.findById(30L)).thenReturn(Optional.of(book));
        when(cartRepository.findItemResponsesByCartId(20L))
                .thenReturn(List.of(itemResponse(60L, 30L, 1, "12.50")));

        CartResponse response =
                cartService.addItem(USERNAME, new AddCartItemRequest(30L, 1));

        assertEquals(20L, response.id());
        assertEquals(1, response.items().size());
        verify(userRepository).findByUsernameForUpdate(USERNAME);
    }

    @Test
    void shouldRejectAddWhenRequestOrRequiredFieldsAreMissing() {
        assertThrows(IllegalArgumentException.class, () -> cartService.addItem(USERNAME, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItem(USERNAME, new AddCartItemRequest(null, 1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItem(USERNAME, new AddCartItemRequest(0L, 1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItem(USERNAME, new AddCartItemRequest(30L, null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addItem(USERNAME, new AddCartItemRequest(30L, 0)));
        verifyNoInteractions(cartRepository, userRepository, bookRepository);
    }

    @Test
    void shouldUpdateExistingItemAtStockBoundary() {
        useLockedCart();
        CartItemEntity item = cartItem(50L, 3);
        cart.addItem(item);
        when(cartRepository.findItemResponsesByCartId(20L)).thenReturn(
                List.of(itemResponse(50L, 30L, 6, "12.50")));

        CartResponse response =
                cartService.updateItem(USERNAME, 50L, new UpdateCartItemRequest(6));

        assertEquals(6, item.getQuantity());
        assertEquals(new BigDecimal("75.00"), response.total());
        verify(cartRepository).save(cart);
    }

    @Test
    void shouldRejectUpdateWithMissingItemOrInsufficientStock() {
        useLockedCart();
        assertThrows(
                CartItemNotFoundException.class,
                () -> cartService.updateItem(USERNAME, 999L, new UpdateCartItemRequest(1)));
        CartItemEntity item = cartItem(50L, 1);
        cart.addItem(item);

        assertThrows(
                InsufficientBookStockException.class,
                () -> cartService.updateItem(USERNAME, 50L, new UpdateCartItemRequest(7)));
        verify(cartRepository, never()).save(any(CartEntity.class));
    }

    @Test
    void shouldRejectUpdateWithMissingOrInvalidArguments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateItem(USERNAME, null, new UpdateCartItemRequest(1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateItem(USERNAME, 1L, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateItem(USERNAME, 1L, new UpdateCartItemRequest(null)));
        assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateItem(USERNAME, 1L, new UpdateCartItemRequest(0)));
        verifyNoInteractions(cartRepository, userRepository, bookRepository);
    }

    @Test
    void shouldRemoveExistingItemAndReturnEmptyCart() {
        useLockedCart();
        CartItemEntity item = cartItem(50L, 2);
        cart.addItem(item);
        when(cartRepository.findItemResponsesByCartId(20L)).thenReturn(List.of());

        CartResponse response = cartService.removeItem(USERNAME, 50L);

        assertEquals(List.of(), cart.getItems());
        assertEquals(BigDecimal.ZERO, response.total());
        verify(cartRepository).save(cart);
    }

    @Test
    void shouldRejectRemoveWhenItemDoesNotExist() {
        useLockedCart();

        assertThrows(CartItemNotFoundException.class, () -> cartService.removeItem(USERNAME, 99L));
        verify(cartRepository, never()).save(any(CartEntity.class));
    }

    @Test
    void shouldRejectRemoveWithNullItemId() {
        assertThrows(IllegalArgumentException.class, () -> cartService.removeItem(USERNAME, null));
        verifyNoInteractions(cartRepository, userRepository, bookRepository);
    }

    private void useLockedCart() {
        when(cartRepository.findByUser_UsernameForUpdate(USERNAME)).thenReturn(Optional.of(cart));
    }

    private CartItemEntity cartItem(Long id, int quantity) {
        CartItemEntity item = new CartItemEntity(cart, book, quantity);
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }

    private BookEntity book(Long id, int stock) {
        BookEntity entity = new BookEntity(
                "Test Book",
                "Test Author",
                "9780000000030",
                "Testing",
                "Test description",
                new BigDecimal("12.50"),
                stock);
        entity.setId(id);
        return entity;
    }

    private CartItemResponse itemResponse(
            Long id, Long bookId, int quantity, String unitPrice) {
        BigDecimal price = new BigDecimal(unitPrice);
        return new CartItemResponse(
                id,
                bookId,
                "Test Book",
                quantity,
                price,
                price.multiply(BigDecimal.valueOf(quantity)));
    }
}
