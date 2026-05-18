package com.cms.backend.activity;

import com.cms.backend.account.UserAccount;
import com.cms.backend.customer.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_contacts")
public class CustomerContact {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_member_id", nullable = false)
    private UserAccount salesMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private Instant contactTime;

    @Column(nullable = false, length = 4000)
    private String communicationSummary;

    protected CustomerContact() {
    }

    public CustomerContact(
            UserAccount salesMember,
            Customer customer,
            Instant contactTime,
            String communicationSummary) {
        this.id = UUID.randomUUID().toString();
        this.salesMember = salesMember;
        this.customer = customer;
        this.contactTime = contactTime;
        this.communicationSummary = communicationSummary;
    }

    public String getId() {
        return id;
    }

    public UserAccount getSalesMember() {
        return salesMember;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Instant getContactTime() {
        return contactTime;
    }

    public String getCommunicationSummary() {
        return communicationSummary;
    }
}
