package com.naukri.bot.service;

import com.naukri.bot.dto.AuthDtos.AuthResponse;
import com.naukri.bot.dto.AuthDtos.LoginRequest;
import com.naukri.bot.repository.UserRepository;
import com.naukri.bot.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);
        String role = userRepository.findByEmailIgnoreCase(request.email()).orElseThrow().getRole().name();
        return new AuthResponse(token, userDetails.getUsername(), role);
    }
}
