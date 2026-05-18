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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "planned_contacts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_planned_contact_member_customer_date",
                columnNames = {"sales_member_id", "customer_id", "planned_date"}))
public class PlannedContact {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_member_id", nullable = false)
    private UserAccount salesMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private LocalDate plannedDate;

    protected PlannedContact() {
    }

    public PlannedContact(UserAccount salesMember, Customer customer, LocalDate plannedDate) {
        this.id = UUID.randomUUID().toString();
        this.salesMember = salesMember;
        this.customer = customer;
        this.plannedDate = plannedDate;
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

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void changePlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }
}
