package com.shootspot.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shootspot.dto.AuthResponse;
import com.shootspot.dto.ChangeEmailRequest;
import com.shootspot.dto.ChangePasswordRequest;
import com.shootspot.dto.LoginRequest;
import com.shootspot.dto.RegisterRequest;
import com.shootspot.dto.UserResponse;
import com.shootspot.dto.UpdateProfileRequest;
import com.shootspot.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse currentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return authService.getCurrentUser(userDetails.getUsername());
    }

    @PutMapping("/me")
    public UserResponse updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return authService.updateCurrentUser(userDetails.getUsername(), request);
    }

    @PutMapping("/me/email")
    public UserResponse changeCurrentUserEmail(
            @Valid @RequestBody ChangeEmailRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return authService.changeCurrentUserEmail(userDetails.getUsername(), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeCurrentUserPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        authService.changeCurrentUserPassword(userDetails.getUsername(), request);
    }
}
