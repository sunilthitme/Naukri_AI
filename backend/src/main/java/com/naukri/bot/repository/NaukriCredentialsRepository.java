package com.naukri.bot.repository;

import com.naukri.bot.domain.NaukriCredentials;
import com.naukri.bot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NaukriCredentialsRepository extends JpaRepository<NaukriCredentials, Long> {
    Optional<NaukriCredentials> findByUser(User user);
}
