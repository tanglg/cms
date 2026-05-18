package com.cms.backend.auth;

import com.cms.backend.account.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "auth_sessions")
public class AuthSession {
    @Id
    private String token;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @Column(nullable = false)
    private Instant createdAt;

    protected AuthSession() {
    }

    public AuthSession(String token, UserAccount account, Instant createdAt) {
        this.token = token;
        this.account = account;
        this.createdAt = createdAt;
    }

    public String getToken() {
        return token;
    }

    public UserAccount getAccount() {
        return account;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
