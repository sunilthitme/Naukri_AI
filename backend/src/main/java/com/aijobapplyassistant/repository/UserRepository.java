package com.aijobapplyassistant.repository;

import com.aijobapplyassistant.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    java.util.List<User> findByActiveTrue();
}
