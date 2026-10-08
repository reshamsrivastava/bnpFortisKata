package com.bnppf.bookstore_be;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM books WHERE isbn = '9780000000001'",
        "INSERT INTO books (id, title, author, isbn, category, description, price, stock) "
                + "VALUES (987654321, 'Test Driven Development', 'Kent Beck', "
                + "'9780000000001', 'Software', 'A test catalog entry', 31.50, 7)"
})
@Sql(statements = "DELETE FROM books WHERE isbn = '9780000000001'",
        executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnCatalogBooks() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.isbn == '9780000000001')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.isbn == '9780000000001')].title")
                        .value(org.hamcrest.Matchers.contains("Test Driven Development")))
                .andExpect(jsonPath("$[?(@.isbn == '9780000000001')].price")
                        .value(org.hamcrest.Matchers.contains(31.50)));
    }

    @Test
    void shouldReturnBookById() throws Exception {
        mockMvc.perform(get("/api/books/{id}", 987654321))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(987654321))
                .andExpect(jsonPath("$.isbn").value("9780000000001"))
                .andExpect(jsonPath("$.title").value("Test Driven Development"))
                .andExpect(jsonPath("$.price").value(31.50));
    }

    @Test
    void shouldReturnNotFoundWhenBookIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/books/{id}", 987654322))
                .andExpect(status().isNotFound());
    }
}