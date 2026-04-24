package com.aijobapplyassistant.service;

import com.aijobapplyassistant.entity.PortalCredential;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.entity.enums.PortalType;
import com.aijobapplyassistant.repository.PortalCredentialRepository;
import com.aijobapplyassistant.service.security.CredentialCipherService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PortalCredentialService {

    private final PortalCredentialRepository repository;
    private final CredentialCipherService credentialCipherService;

    public PortalCredentialService(PortalCredentialRepository repository, CredentialCipherService credentialCipherService) {
        this.repository = repository;
        this.credentialCipherService = credentialCipherService;
    }

    public List<PortalCredential> findForUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<PortalCredential> findByPortal(Long userId, String portalUrl) {
        return repository.findByUserIdAndPortalUrl(userId, portalUrl);
    }

    public PortalCredential save(User user, String companyName, String portalUrl, String username, String password, PortalType portalType) {
        PortalCredential credential = repository.findByUserIdAndPortalUrl(user.getId(), portalUrl).orElse(new PortalCredential());
        credential.setUser(user);
        credential.setCompanyName(companyName);
        credential.setPortalUrl(portalUrl);
        credential.setUsername(username);
        credential.setEncryptedPassword(credentialCipherService.encrypt(password));
        credential.setPortalType(portalType);
        return repository.save(credential);
    }

    public String decryptPassword(PortalCredential credential) {
        return credentialCipherService.decrypt(credential.getEncryptedPassword());
    }
}
