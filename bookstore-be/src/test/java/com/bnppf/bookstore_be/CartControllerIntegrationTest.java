package com.bnppf.bookstore_be;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM cart_items WHERE cart_id IN "
                + "(SELECT c.id FROM carts c JOIN users u ON c.user_id = u.id "
                + "WHERE u.username IN ('resham', 'seconduser'))",
        "DELETE FROM carts WHERE user_id IN "
                + "(SELECT id FROM users WHERE username IN ('resham', 'seconduser'))",
        "DELETE FROM users WHERE username IN ('resham', 'seconduser')",
        "DELETE FROM books WHERE isbn = '9780000000001'",
        "INSERT INTO books (id, title, author, isbn, category, description, price, stock) "
                + "VALUES (99887766, 'Test Driven Development', 'Kent Beck', "
                + "'9780000000001', 'Software', 'Cart test book', 31.50, 7)"
})
@Sql(statements = {
        "DELETE FROM cart_items WHERE cart_id IN "
                + "(SELECT c.id FROM carts c JOIN users u ON c.user_id = u.id "
                + "WHERE u.username IN ('resham', 'seconduser'))",
        "DELETE FROM carts WHERE user_id IN "
                + "(SELECT id FROM users WHERE username IN ('resham', 'seconduser'))",
        "DELETE FROM users WHERE username IN ('resham', 'seconduser')",
        "DELETE FROM books WHERE isbn = '9780000000001'"
}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String authorization;

    @BeforeEach
    void registerCartOwner() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "resham",
                                  "email": "resham@example.com",
                                  "password": "securePassword123"
                                }
                                """))
                .andExpect(status().isCreated());
        authorization = basicAuthorization("resham", "securePassword123");
    }

    @Test
    void shouldAddBookAndReturnCurrentUsersCart() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookId": 99887766,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].bookId").value(99887766))
                .andExpect(jsonPath("$.items[0].title").value("Test Driven Development"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(31.50))
                .andExpect(jsonPath("$.total").value(63.00));

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": 99887766, "quantity": 1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.total").value(94.50));

        mockMvc.perform(get("/api/cart").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(3));
    }

    @Test
    void shouldReturnEmptyCartWhenUserHasNoItems() throws Exception {
        mockMvc.perform(get("/api/cart").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void shouldOnlyExposeCartAndItemsToTheirOwner() throws Exception {
        MvcResult addResult = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": 99887766, "quantity": 1}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        int itemId = JsonPath.read(addResult.getResponse().getContentAsString(), "$.items[0].id");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "seconduser",
                                  "email": "seconduser@example.com",
                                  "password": "securePassword123"
                                }
                                """))
                .andExpect(status().isCreated());
        String secondUserAuthorization = basicAuthorization("seconduser", "securePassword123");

        mockMvc.perform(get("/api/cart").header("Authorization", secondUserAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));

        mockMvc.perform(put("/api/cart/items/{id}", itemId)
                        .header("Authorization", secondUserAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 3}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/cart/items/{id}", itemId)
                        .header("Authorization", secondUserAuthorization))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/cart").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(itemId))
                .andExpect(jsonPath("$.items[0].quantity").value(1));
    }

    @Test
    void shouldUpdateItemQuantity() throws Exception {
        MvcResult addResult = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": 99887766, "quantity": 1}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        int itemId = JsonPath.read(addResult.getResponse().getContentAsString(), "$.items[0].id");

        mockMvc.perform(put("/api/cart/items/{id}", itemId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(4))
                .andExpect(jsonPath("$.total").value(126.00));
    }

    @Test
    void shouldRemoveItemFromCart() throws Exception {
        MvcResult addResult = mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": 99887766, "quantity": 1}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        int itemId = JsonPath.read(addResult.getResponse().getContentAsString(), "$.items[0].id");

        mockMvc.perform(delete("/api/cart/items/{id}", itemId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void shouldRejectInvalidQuantityAndUnknownBook() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": 99887766, "quantity": 0}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": 123456789, "quantity": 1}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/cart/items")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": 99887766, "quantity": 8}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireAuthenticationForCart() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    private String basicAuthorization(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
