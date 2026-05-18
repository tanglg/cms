package com.cms.backend.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean salesMember;

    @Column(nullable = false)
    private boolean manager;

    @Column(nullable = false)
    private boolean active;

    protected UserAccount() {
    }

    public UserAccount(String name, String phoneNumber, String passwordHash, boolean salesMember, boolean manager) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.salesMember = salesMember;
        this.manager = manager;
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isSalesMember() {
        return salesMember;
    }

    public boolean isManager() {
        return manager;
    }

    public boolean isActive() {
        return active;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeRoles(boolean salesMember, boolean manager) {
        this.salesMember = salesMember;
        this.manager = manager;
    }

    public void deactivate() {
        this.active = false;
    }
}
