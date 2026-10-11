package com.bnppf.bookstore_be.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bnppf.bookstore_be.exception.UserAlreadyExistsException;
import com.bnppf.bookstore_be.jpa.entity.UserEntity;
import com.bnppf.bookstore_be.jpa.repository.UserRepository;
import com.bnppf.bookstore_be.records.user.RegisterUserRequest;
import com.bnppf.bookstore_be.records.user.UserResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterUserRequest request;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        request = new RegisterUserRequest("reader", "reader@example.com", "plainPassword");
        user = new UserEntity("reader", "reader@example.com", "encodedPassword");
        ReflectionTestUtils.setField(user, "id", 7L);
    }

    @Test
    void shouldEncodePasswordAndReturnSavedUserWithoutPassword() {
        when(userRepository.existsByUsername("reader")).thenReturn(false);
        when(userRepository.existsByEmail("reader@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(user);

        UserResponse response = userService.register(request);

        assertEquals(new UserResponse(7L, "reader", "reader@example.com"), response);
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals("encodedPassword", captor.getValue().getPassword());
        verify(passwordEncoder).encode("plainPassword");
    }

    @Test
    void shouldRejectDuplicateUsernameWithoutCheckingEmail() {
        when(userRepository.existsByUsername("reader")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.register(request));

        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectDuplicateEmail() {
        when(userRepository.existsByUsername("reader")).thenReturn(false);
        when(userRepository.existsByEmail("reader@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.register(request));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectNullRegistrationRequest() {
        assertThrows(IllegalArgumentException.class, () -> userService.register(null));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void shouldReturnUserByUsername() {
        when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));

        assertEquals(
                new UserResponse(7L, "reader", "reader@example.com"),
                userService.getUserByUsername("reader"));
        verify(userRepository).findByUsername("reader");
    }

    @Test
    void shouldFailWhenAuthenticatedUserIsMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> userService.getUserByUsername("missing"));
        assertEquals("Authenticated user was not found", exception.getMessage());
    }

    @Test
    void shouldRejectBlankUsernameLookup() {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserByUsername(" "));
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectNullUsernameLookup() {
        assertThrows(IllegalArgumentException.class, () -> userService.getUserByUsername(null));
        verifyNoInteractions(userRepository);
    }
}
