package com.naukri.bot.config;

import com.naukri.bot.domain.User;
import com.naukri.bot.domain.UserRole;
import com.naukri.bot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapAdmin implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String email = properties.bootstrap().adminEmail();
        String password = properties.bootstrap().adminPassword();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(user -> {
        }, () -> {
            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(UserRole.ROLE_ADMIN);
            userRepository.save(user);
            log.info("Bootstrap admin user created for {}", email);
        });
    }
}
