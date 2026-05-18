package com.cms.backend.customer;

import com.cms.backend.account.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomerStatus status;

    @Column(nullable = false)
    private int attentionLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    private LocalDate agreementSigningDate;

    @Enumerated(EnumType.STRING)
    private CustomerStatus statusBeforeInactive;

    protected Customer() {
    }

    public Customer(String name, UserAccount owner) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.owner = owner;
        this.status = CustomerStatus.PROSPECTIVE_CUSTOMER;
        this.attentionLevel = 1;
    }

    public Customer(String name, CustomerStatus status, UserAccount owner, LocalDate agreementSigningDate) {
        if (status == null) {
            throw new IllegalArgumentException("客户状态不能为空");
        }
        if (status == CustomerStatus.INACTIVE_CUSTOMER) {
            throw new IllegalArgumentException("不能直接创建停用客户");
        }
        if (status == CustomerStatus.FORMAL_CUSTOMER && agreementSigningDate == null) {
            throw new IllegalArgumentException("正式客户必须填写协议签署日期");
        }
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.owner = owner;
        this.status = status;
        this.agreementSigningDate = agreementSigningDate;
        this.attentionLevel = 1;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public int getAttentionLevel() {
        return attentionLevel;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public LocalDate getAgreementSigningDate() {
        return agreementSigningDate;
    }

    public void changeAttentionLevel(int attentionLevel) {
        if (attentionLevel < 1 || attentionLevel > 5) {
            throw new IllegalArgumentException("关注程度必须在一到五级之间");
        }
        this.attentionLevel = attentionLevel;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeOwner(UserAccount owner) {
        this.owner = owner;
    }

    public void deactivate() {
        if (status == CustomerStatus.INACTIVE_CUSTOMER) {
            return;
        }
        this.statusBeforeInactive = status;
        this.status = CustomerStatus.INACTIVE_CUSTOMER;
    }

    public void restore() {
        if (status != CustomerStatus.INACTIVE_CUSTOMER) {
            return;
        }
        this.status = statusBeforeInactive == null ? CustomerStatus.PROSPECTIVE_CUSTOMER : statusBeforeInactive;
        this.statusBeforeInactive = null;
    }
}
