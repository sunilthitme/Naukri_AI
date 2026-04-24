package com.aijobapplyassistant.controller;

import com.aijobapplyassistant.dto.auth.AuthResponse;
import com.aijobapplyassistant.dto.auth.LoginRequest;
import com.aijobapplyassistant.dto.auth.RegisterRequest;
import com.aijobapplyassistant.dto.common.ApiResponse;
import com.aijobapplyassistant.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), "Registration successful");
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), "Login successful");
    }
}
