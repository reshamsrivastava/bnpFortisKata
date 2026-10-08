package com.bnppf.bookstore_be;

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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
        "DELETE FROM users WHERE username IN ('resham', 'resham2') "
                + "OR email = 'resham@example.com'"
})
@Sql(statements = {
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

        mockMvc.perform(post("/api/users/login")
                        .header("Authorization", basicAuthorization("resham", "securePassword123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("resham"))
                .andExpect(jsonPath("$.email").value("resham@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
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
                        .header("Authorization", basicAuthorization("resham", "wrongPassword123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireBasicAuthenticationToLogin() throws Exception {
        mockMvc.perform(post("/api/users/login"))
                .andExpect(status().isUnauthorized());
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
