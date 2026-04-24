package com.aijobapplyassistant.repository;

import com.aijobapplyassistant.entity.SystemLogEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemLogRepository extends JpaRepository<SystemLogEntry, Long> {

    List<SystemLogEntry> findTop200ByUserIdOrderByOccurredAtDesc(Long userId);
}
