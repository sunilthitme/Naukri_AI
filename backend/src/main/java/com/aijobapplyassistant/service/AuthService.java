package com.aijobapplyassistant.service;

import com.aijobapplyassistant.dto.auth.AuthResponse;
import com.aijobapplyassistant.dto.auth.LoginRequest;
import com.aijobapplyassistant.dto.auth.RegisterRequest;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.entity.enums.Role;
import com.aijobapplyassistant.exception.ApiException;
import com.aijobapplyassistant.repository.UserRepository;
import com.aijobapplyassistant.service.security.JwtService;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserProfileService userProfileService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            UserProfileService userProfileService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userProfileService = userProfileService;
    }

    public AuthResponse register(RegisterRequest request) {
        Optional<User> existing = userRepository.findByEmailIgnoreCase(request.email());
        if (existing.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "User already exists");
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        userRepository.save(user);
        return createAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return createAuthResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, jwtService.getExpiry(token), userProfileService.toResponse(user));
    }
}
