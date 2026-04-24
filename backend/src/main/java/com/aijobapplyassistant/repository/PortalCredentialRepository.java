package com.aijobapplyassistant.repository;

import com.aijobapplyassistant.entity.PortalCredential;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortalCredentialRepository extends JpaRepository<PortalCredential, Long> {

    List<PortalCredential> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<PortalCredential> findByUserIdAndPortalUrl(Long userId, String portalUrl);
}
