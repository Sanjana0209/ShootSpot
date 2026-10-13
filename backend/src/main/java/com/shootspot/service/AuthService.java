package com.shootspot.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.shootspot.dto.AuthResponse;
import com.shootspot.dto.ChangeEmailRequest;
import com.shootspot.dto.ChangePasswordRequest;
import com.shootspot.dto.LoginRequest;
import com.shootspot.dto.RegisterRequest;
import com.shootspot.dto.UserResponse;
import com.shootspot.dto.UpdateProfileRequest;
import com.shootspot.entity.User;
import com.shootspot.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw emailAlreadyRegistered();
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setPhone(request.phone());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("USER");

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return toUserResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw emailAlreadyRegistered();
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(foundUser -> passwordEncoder.matches(
                        request.password(), foundUser.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return new AuthResponse("Bearer", jwtService.createToken(user), toUserResponse(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authentication required"));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(String authenticatedEmail, UpdateProfileRequest request) {
        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authentication required"));
        user.setName(request.name());
        user.setPhone(request.phone());
        return toUserResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse changeCurrentUserEmail(String authenticatedEmail, ChangeEmailRequest request) {
        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authentication required"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        String newEmail = normalizeEmail(request.email());
        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "New email must be different from your current email");
        }
        if (userRepository.existsByEmailIgnoreCase(newEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        user.setEmail(newEmail);
        try {
            return toUserResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
    }

    @Transactional
    public void changeCurrentUserPassword(
            String authenticatedEmail, ChangePasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authentication required"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New passwords do not match");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.saveAndFlush(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getPhone());
    }

    private ResponseStatusException emailAlreadyRegistered() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
    }
}
