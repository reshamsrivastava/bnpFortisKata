package com.bnppf.bookstore_be.controller;

import com.bnppf.bookstore_be.records.error.ApiErrorResponse;
import com.bnppf.bookstore_be.records.user.LoginUserRequest;
import com.bnppf.bookstore_be.records.user.RegisterUserRequest;
import com.bnppf.bookstore_be.records.user.UserResponse;
import com.bnppf.bookstore_be.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginUserRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(), request.password()));

            if (httpRequest.getSession(false) != null) {
                httpRequest.changeSessionId();
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            UserResponse user = userService.getUserByUsername(authentication.getName());
            log.atInfo()
                    .addKeyValue("event", "user.sign_in.succeeded")
                    .addKeyValue("username", user.username())
                    .log("User signed in");
            return ResponseEntity.ok(user);
        } catch (AuthenticationException exception) {
            log.atWarn()
                    .addKeyValue("event", "user.sign_in.failed")
                    .addKeyValue("username", request.username())
                    .log("User sign-in failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiErrorResponse.of(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid username or password",
                            httpRequest.getRequestURI()));
        }
    }
}
