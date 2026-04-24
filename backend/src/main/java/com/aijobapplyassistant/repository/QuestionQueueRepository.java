package com.aijobapplyassistant.repository;

import com.aijobapplyassistant.entity.QuestionQueueItem;
import com.aijobapplyassistant.entity.enums.QuestionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionQueueRepository extends JpaRepository<QuestionQueueItem, Long> {

    List<QuestionQueueItem> findByUserIdAndStatusOrderByCreatedAtAsc(Long userId, QuestionStatus status);
}
