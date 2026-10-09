package com.bnppf.bookstore_be;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM cart_items WHERE cart_id IN "
                + "(SELECT c.id FROM carts c JOIN users u ON c.user_id = u.id "
                + "WHERE u.username IN ('resham', 'resham2'))",
        "DELETE FROM carts WHERE user_id IN "
                + "(SELECT id FROM users WHERE username IN ('resham', 'resham2'))",
        "DELETE FROM users WHERE username IN ('resham', 'resham2') "
                + "OR email = 'resham@example.com'"
})
@Sql(statements = {
        "DELETE FROM cart_items WHERE cart_id IN "
                + "(SELECT c.id FROM carts c JOIN users u ON c.user_id = u.id "
                + "WHERE u.username IN ('resham', 'resham2'))",
        "DELETE FROM carts WHERE user_id IN "
                + "(SELECT id FROM users WHERE username IN ('resham', 'resham2'))",
        "DELETE FROM users WHERE username IN ('resham', 'resham2') "
                + "OR email = 'resham@example.com'"
}, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRegisterUserAndAllowLoginWithoutReturningPassword() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "resham",
                                  "email": "resham@example.com",
                                  "password": "securePassword123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("resham"))
                .andExpect(jsonPath("$.email").value("resham@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "resham",
                                  "password": "securePassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("resham"))
                .andExpect(jsonPath("$.email").value("resham@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/api/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void shouldAuthenticateSeededDemoUserWithJsonCredentials() throws Exception {
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "demo",
                                  "password": "DemoPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.email").value("demo@bookstore.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldAllowRegistrationWhenClientSendsStaleBasicCredentials() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .header("Authorization", basicAuthorization("old-user", "old-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "resham",
                                  "email": "resham@example.com",
                                  "password": "securePassword123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("resham"));
    }

    @Test
    void shouldAllowSwaggerAndOpenApiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")
                        .header("Authorization", basicAuthorization("old-user", "old-password")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs")
                        .header("Authorization", basicAuthorization("old-user", "old-password")))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDocumentJsonCredentialsForLoginInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/users/login'].post.requestBody").exists())
                .andExpect(jsonPath("$.paths['/api/users/login'].post.security").doesNotExist());
    }

    @Test
    void shouldRejectInvalidRegistration() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "not-an-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateUsernameOrEmail() throws Exception {
        String request = """
                {
                  "username": "resham",
                  "email": "resham@example.com",
                  "password": "securePassword123"
                }
                """;

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
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

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "resham2",
                                  "email": "resham@example.com",
                                  "password": "securePassword123"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
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

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "resham",
                                  "password": "wrongPassword123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectMissingLoginFields() throws Exception {
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "", "password": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowCorsPreflightFromFrontend() throws Exception {
        mockMvc.perform(options("/api/users/login")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:8081"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Credentials", "true"));
    }

    private String basicAuthorization(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
