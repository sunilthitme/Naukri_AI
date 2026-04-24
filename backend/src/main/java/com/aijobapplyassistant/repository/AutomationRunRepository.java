package com.aijobapplyassistant.repository;

import com.aijobapplyassistant.entity.AutomationRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationRunRepository extends JpaRepository<AutomationRun, Long> {

    Optional<AutomationRun> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    List<AutomationRun> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
