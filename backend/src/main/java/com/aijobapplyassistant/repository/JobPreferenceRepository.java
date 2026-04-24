package com.aijobapplyassistant.repository;

import com.aijobapplyassistant.entity.JobPreference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPreferenceRepository extends JpaRepository<JobPreference, Long> {

    List<JobPreference> findByUserIdAndActiveTrue(Long userId);
}
