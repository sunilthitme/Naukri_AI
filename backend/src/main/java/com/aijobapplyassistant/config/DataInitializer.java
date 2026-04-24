package com.aijobapplyassistant.config;

import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.entity.enums.Role;
import com.aijobapplyassistant.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> userRepository.findByEmailIgnoreCase("admin@aijobapply.local").orElseGet(() -> {
            User user = new User();
            user.setFullName("Admin User");
            user.setEmail("admin@aijobapply.local");
            user.setPhoneNumber("9999999999");
            user.setPasswordHash(passwordEncoder.encode("Admin@123"));
            user.setRole(Role.ADMIN);
            return userRepository.save(user);
        });
    }
}
