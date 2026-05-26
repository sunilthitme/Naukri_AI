package com.naukri.bot.repository;

import com.naukri.bot.domain.QuestionAnswer;
import com.naukri.bot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    List<QuestionAnswer> findByUserOrderByUpdatedAtDesc(User user);

    Optional<QuestionAnswer> findByUserAndNormalizedQuestion(User user, String normalizedQuestion);
}
