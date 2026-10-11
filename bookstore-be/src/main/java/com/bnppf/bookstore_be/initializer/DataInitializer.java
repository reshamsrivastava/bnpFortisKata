package com.bnppf.bookstore_be.initializer;

import com.bnppf.bookstore_be.jpa.entity.BookEntity;
import com.bnppf.bookstore_be.jpa.entity.CartEntity;
import com.bnppf.bookstore_be.jpa.entity.CartItemEntity;
import com.bnppf.bookstore_be.jpa.entity.OrderEntity;
import com.bnppf.bookstore_be.jpa.entity.OrderItemEntity;
import com.bnppf.bookstore_be.jpa.entity.UserEntity;
import com.bnppf.bookstore_be.jpa.repository.BookRepository;
import com.bnppf.bookstore_be.jpa.repository.CartRepository;
import com.bnppf.bookstore_be.jpa.repository.OrderRepository;
import com.bnppf.bookstore_be.jpa.repository.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String INITIAL_DATA_RESOURCE = "data/initial-data.json";

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) {
        InitialData initialData = readInitialData();
        seedBooks(initialData.books());
        UserEntity demoUser = seedDemoUser(initialData.user());
        seedDemoCart(demoUser, initialData.cart());
        seedDemoOrders(demoUser, initialData.orders());
    }

    private InitialData readInitialData() {
        try (InputStream input = new ClassPathResource(INITIAL_DATA_RESOURCE).getInputStream()) {
            return objectMapper.readValue(input, InitialData.class);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load startup data from " + INITIAL_DATA_RESOURCE, exception);
        }
    }

    private void seedBooks(List<BookData> books) {
        List<BookEntity> missingBooks = books.stream()
                .filter(book -> !bookRepository.existsByIsbn(book.isbn()))
                .map(book -> new BookEntity(
                        book.title(),
                        book.author(),
                        book.isbn(),
                        book.category(),
                        book.description(),
                        book.price(),
                        book.stock()))
                .toList();
        bookRepository.saveAll(missingBooks);
    }

    private UserEntity seedDemoUser(UserData userData) {
        return userRepository.findByUsername(userData.username())
                .orElseGet(() -> userRepository.save(new UserEntity(
                        userData.username(),
                        userData.email(),
                        passwordEncoder.encode(userData.password()))));
    }

    private void seedDemoCart(UserEntity user, CartData cartData) {
        CartEntity cart = cartRepository.findByUser_Username(user.getUsername())
                .orElseGet(() -> new CartEntity(user));

        if (cart.getItems().isEmpty()) {
            for (CartItemData itemData : cartData.items()) {
                BookEntity book = bookRepository.findByIsbn(itemData.isbn())
                        .orElseThrow(() -> new IllegalStateException(
                                "Startup cart references unknown ISBN: " + itemData.isbn()));
                cart.addItem(new CartItemEntity(cart, book, itemData.quantity()));
            }
        }

        cartRepository.save(cart);
    }

    private void seedDemoOrders(UserEntity user, List<OrderData> orders) {
        for (OrderData orderData : orders) {
            if (orderRepository.findByUser_UsernameAndIdempotencyKey(
                    user.getUsername(), orderData.idempotencyKey()).isPresent()) {
                continue;
            }

            OrderEntity order = new OrderEntity(user, orderData.idempotencyKey());
            for (OrderItemData itemData : orderData.items()) {
                BookEntity book = bookRepository.findByIsbn(itemData.isbn())
                        .orElseThrow(() -> new IllegalStateException(
                                "Startup order references unknown ISBN: " + itemData.isbn()));
                order.addItem(new OrderItemEntity(
                        order, book, itemData.quantity(), itemData.unitPrice()));
            }
            order.complete();
            orderRepository.save(order);
        }
    }

    public record InitialData(
            List<BookData> books, UserData user, CartData cart, List<OrderData> orders) {
    }

    public record BookData(
            String title,
            String author,
            String isbn,
            String category,
            String description,
            BigDecimal price,
            int stock) {
    }

    public record UserData(String username, String email, String password) {
    }

    public record CartData(List<CartItemData> items) {
    }

    public record CartItemData(String isbn, int quantity) {
    }

    public record OrderData(String idempotencyKey, List<OrderItemData> items) {
    }

    public record OrderItemData(String isbn, int quantity, BigDecimal unitPrice) {
    }
}
