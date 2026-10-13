package com.shootspot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.shootspot.dto.AuthResponse;
import com.shootspot.dto.ChangeEmailRequest;
import com.shootspot.dto.ChangePasswordRequest;
import com.shootspot.dto.LoginRequest;
import com.shootspot.dto.RegisterRequest;
import com.shootspot.dto.UpdateProfileRequest;
import com.shootspot.dto.UserResponse;
import com.shootspot.entity.User;
import com.shootspot.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerHashesPasswordAndAlwaysAssignsUserRole() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserResponse response = authService.register(
                new RegisterRequest("Person", " PERSON@EXAMPLE.COM ", "9876543210", "password123"));

        assertEquals("person@example.com", response.email());
        assertEquals("USER", response.role());
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(savedUser.capture());
        assertNotEquals("password123", savedUser.getValue().getPasswordHash());
        assertTrue(passwordEncoder.matches("password123", savedUser.getValue().getPasswordHash()));
        assertEquals("USER", savedUser.getValue().getRole());
        assertEquals("9876543210", savedUser.getValue().getPhone());
        assertEquals("9876543210", response.phone());
    }

    @Test
    void registerRejectsDuplicateEmailWithConflict() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.register(
                        new RegisterRequest("Person", "person@example.com", "9876543210", "password123")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void loginReturnsBearerTokenForValidCredentials() {
        User user = new User();
        user.setId(1L);
        user.setName("Person");
        user.setEmail("person@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole("USER");
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(jwtService.createToken(user)).thenReturn("signed.jwt.token");

        AuthResponse response = authService.login(
                new LoginRequest("PERSON@example.com", "password123"));

        assertEquals("Bearer", response.tokenType());
        assertEquals("signed.jwt.token", response.token());
        assertEquals("USER", response.user().role());
    }

    @Test
    void loginCanonicalizesRoleWithSpringPrefix() {
        User user = new User();
        user.setId(2L);
        user.setName("Admin");
        user.setEmail("admin@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole("ROLE_ADMIN");
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user));
        when(jwtService.createToken(user)).thenReturn("signed.jwt.token");

        AuthResponse response = authService.login(
                new LoginRequest("admin@example.com", "password123"));

        assertEquals("ADMIN", response.user().role());
    }

    @Test
    void loginRejectsIncorrectPasswordWithUnauthorized() {
        User user = new User();
        user.setEmail("person@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(
                        new LoginRequest("person@example.com", "wrong-password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void updateCurrentUserChangesOnlyNameAndPhoneForAuthenticatedAccount() {
        User user = new User();
        user.setId(17L);
        user.setName("Before Name");
        user.setEmail("person@example.com");
        user.setPhone("9876543210");
        user.setPasswordHash("existing-hash");
        user.setRole("USER");
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserResponse response = authService.updateCurrentUser(
                "person@example.com", new UpdateProfileRequest("  Updated Name  ", "9123456780"));

        assertEquals("Updated Name", response.name());
        assertEquals("9123456780", response.phone());
        assertEquals("person@example.com", response.email());
        assertEquals("USER", response.role());
        assertEquals("existing-hash", user.getPasswordHash());
        verify(userRepository).findByEmailIgnoreCase("person@example.com");
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void changeEmailRequiresCurrentPasswordAndPersistsNormalizedEmail() {
        User user = createUserForCredentialChange();
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        UserResponse response = authService.changeCurrentUserEmail(
                "person@example.com",
                new ChangeEmailRequest(" NEW@EXAMPLE.COM ", "CurrentPass1!"));

        assertEquals("new@example.com", response.email());
        assertEquals("ADMIN", response.role());
        assertEquals("credential-user", user.getName());
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void changeEmailRejectsAnEmailAlreadyInUse() {
        User user = createUserForCredentialChange();
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.changeCurrentUserEmail(
                        "person@example.com",
                        new ChangeEmailRequest("new@example.com", "CurrentPass1!")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void changePasswordHashesPasswordAndIncrementsTokenVersion() {
        User user = createUserForCredentialChange();
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        authService.changeCurrentUserPassword(
                "person@example.com",
                new ChangePasswordRequest("CurrentPass1!", "NewPass2@", "NewPass2@"));

        assertTrue(passwordEncoder.matches("NewPass2@", user.getPasswordHash()));
        assertNotEquals("NewPass2@", user.getPasswordHash());
        assertEquals(1, user.getTokenVersion());
        assertEquals("ADMIN", user.getRole());
        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void changePasswordRejectsAnIncorrectCurrentPassword() {
        User user = createUserForCredentialChange();
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.changeCurrentUserPassword(
                        "person@example.com",
                        new ChangePasswordRequest("WrongPass1!", "NewPass2@", "NewPass2@")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void changePasswordRejectsMismatchedConfirmation() {
        User user = createUserForCredentialChange();
        when(userRepository.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.changeCurrentUserPassword(
                        "person@example.com",
                        new ChangePasswordRequest("CurrentPass1!", "NewPass2@", "Different3#")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private User createUserForCredentialChange() {
        User user = new User();
        user.setId(21L);
        user.setName("credential-user");
        user.setEmail("person@example.com");
        user.setPhone("9876543210");
        user.setPasswordHash(passwordEncoder.encode("CurrentPass1!"));
        user.setRole("ADMIN");
        return user;
    }
}
