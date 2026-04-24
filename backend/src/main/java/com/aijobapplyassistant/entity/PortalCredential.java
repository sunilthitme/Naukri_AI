package com.aijobapplyassistant.entity;

import com.aijobapplyassistant.entity.enums.PortalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "portal_credentials")
public class PortalCredential extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String portalUrl;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, length = 1024)
    private String encryptedPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PortalType portalType = PortalType.CUSTOM;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPortalUrl() {
        return portalUrl;
    }

    public void setPortalUrl(String portalUrl) {
        this.portalUrl = portalUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public PortalType getPortalType() {
        return portalType;
    }

    public void setPortalType(PortalType portalType) {
        this.portalType = portalType;
    }
}
