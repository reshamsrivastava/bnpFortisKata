package com.bnppf.bookstore_be;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM order_items WHERE order_id IN "
                + "(SELECT o.id FROM customer_orders o JOIN users u ON o.user_id = u.id "
                + "WHERE u.username IN ('orderuser', 'orderuser2'))",
        "DELETE FROM customer_orders WHERE user_id IN "
                + "(SELECT id FROM users WHERE username IN ('orderuser', 'orderuser2'))",
        "DELETE FROM cart_items WHERE cart_id IN "
                + "(SELECT c.id FROM carts c JOIN users u ON c.user_id = u.id "
                + "WHERE u.username IN ('orderuser', 'orderuser2'))",
        "DELETE FROM carts WHERE user_id IN "
                + "(SELECT id FROM users WHERE username IN ('orderuser', 'orderuser2'))",
        "DELETE FROM users WHERE username IN ('orderuser', 'orderuser2') "
                + "OR email IN ('orderuser@example.com', 'orderuser2@example.com')",
        "DELETE FROM books WHERE isbn = '9780000000002'",
        "INSERT INTO books (id, title, author, isbn, category, description, price, stock) "
                + "VALUES (99887767, 'Order Testing', 'Kent Beck', "
                + "'9780000000002', 'Software', 'Order test book', 31.50, 7)"
})
@Sql(statements = {
        "DELETE FROM order_items WHERE order_id IN "
                + "(SELECT o.id FROM customer_orders o JOIN users u ON o.user_id = u.id "
                + "WHERE u.username IN ('orderuser', 'orderuser2'))",
        "DELETE FROM customer_orders WHERE user_id IN "
                + "(SELECT id FROM users WHERE username IN ('orderuser', 'orderuser2'))",
        "DELETE FROM cart_items WHERE cart_id IN "
                + "(SELECT c.id FROM carts c JOIN users u ON c.user_id = u.id "
                + "WHERE u.username IN ('orderuser', 'orderuser2'))",
        "DELETE FROM carts WHERE user_id IN "
                + "(SELECT id FROM users WHERE username IN ('orderuser', 'orderuser2'))",
        "DELETE FROM users WHERE username IN ('orderuser', 'orderuser2') "
                + "OR email IN ('orderuser@example.com', 'orderuser2@example.com')",
        "DELETE FROM books WHERE isbn = '9780000000002'"
}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String authorization;

    @BeforeEach
    void registerOrderOwner() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "orderuser",
                                  "email": "orderuser@example.com",
                                  "password": "securePassword123"
                                }
                                """))
                .andExpect(status().isCreated());
        authorization = basicAuthorization("orderuser", "securePassword123");
    }

    @Test
    void shouldCheckoutCartAndReturnSameOrderForAnIdempotentRetry() throws Exception {
        addBookToCart(2);

        MvcResult checkoutResult = mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", "checkout-001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[0].title").value("Order Testing"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(31.50))
                .andExpect(jsonPath("$.total").value(63.00))
                .andReturn();
        String orderId = JsonPath.read(
                checkoutResult.getResponse().getContentAsString(), "$.id").toString();
        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", "checkout-001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.total").value(63.00));

        assertEquals(
                5, jdbcTemplate.queryForObject(
                        "SELECT stock FROM books WHERE id = 99887767", Integer.class));
        assertEquals(
                1, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM customer_orders", Integer.class));
        mockMvc.perform(get("/api/cart").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void shouldRejectCheckoutForAnEmptyCart() throws Exception {
        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", "empty-cart"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLeaveCartAndInventoryUnchangedWhenStockIsInsufficient() throws Exception {
        addBookToCart(5);
        jdbcTemplate.update("UPDATE books SET stock = 1 WHERE id = 99887767");

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", "insufficient-stock"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", authorization)
                        .header("Idempotency-Key", "retry-after-stock-failure"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/cart").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(5));
        assertEquals(
                1, jdbcTemplate.queryForObject(
                        "SELECT stock FROM books WHERE id = 99887767", Integer.class));
    }

    @Test
    void shouldRequireAnIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", authorization))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPreventOversellingWhenDifferentCustomersCheckoutConcurrently() throws Exception {
        String secondUserAuthorization = registerSecondUser();
        addBookToCart(1);
        addBookToCart(secondUserAuthorization, 1);
        jdbcTemplate.update("UPDATE books SET stock = 1 WHERE id = 99887767");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> firstCheckout = submitCheckout(executor, authorization, "race-1");
            Future<Integer> secondCheckout =
                    submitCheckout(executor, secondUserAuthorization, "race-2");
            int firstStatus = firstCheckout.get().intValue();
            int secondStatus = secondCheckout.get().intValue();

            assertEquals(201, (firstStatus == 201 || secondStatus == 201)
                    ? 201 : Math.max(firstStatus, secondStatus));
            assertEquals(400, (firstStatus == 400 || secondStatus == 400)
                    ? 400 : Math.min(firstStatus, secondStatus));
            assertEquals(
                    0, jdbcTemplate.queryForObject(
                            "SELECT stock FROM books WHERE id = 99887767", Integer.class));
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM customer_orders", Integer.class));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldAllowCheckoutCorsPreflightWithIdempotencyHeader() throws Exception {
        mockMvc.perform(options("/api/orders/checkout")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers",
                                "authorization,content-type,idempotency-key"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    private void addBookToCart(int quantity) throws Exception {
        addBookToCart(authorization, quantity);
    }

    private void addBookToCart(String userAuthorization, int quantity) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", userAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": 99887767, "quantity": %d}
                                """.formatted(quantity)))
                .andExpect(status().isCreated());
    }

    private String registerSecondUser() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "orderuser2",
                                  "email": "orderuser2@example.com",
                                  "password": "securePassword123"
                                }
                                """))
                .andExpect(status().isCreated());
        return basicAuthorization("orderuser2", "securePassword123");
    }

    private Future<Integer> submitCheckout(
            ExecutorService executor, String userAuthorization, String idempotencyKey) {
        return executor.submit(() -> mockMvc.perform(post("/api/orders/checkout")
                        .header("Authorization", userAuthorization)
                        .header("Idempotency-Key", idempotencyKey))
                .andReturn()
                .getResponse()
                .getStatus());
    }

    private String basicAuthorization(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
