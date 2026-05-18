package com.cms.backend.activity;

import com.cms.backend.account.UserAccount;
import com.cms.backend.customer.Customer;
import com.cms.backend.customer.CustomerRepository;
import com.cms.backend.customer.CustomerStatus;
import com.cms.backend.security.CurrentAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales/customer-contacts")
@PreAuthorize("hasRole('SALES_MEMBER')")
@Transactional
public class SalesCustomerContactController {
    private final CustomerRepository customers;
    private final CustomerContactRepository customerContacts;

    public SalesCustomerContactController(
            CustomerRepository customers,
            CustomerContactRepository customerContacts) {
        this.customers = customers;
        this.customerContacts = customerContacts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerContactResponse createCustomerContact(
            @AuthenticationPrincipal CurrentAccount current,
            @Valid @RequestBody CreateCustomerContactRequest request) {
        UserAccount salesMember = current.account();
        Customer customer = customers.findByIdAndOwnerId(request.customerId(), salesMember.getId())
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
        if (customer.getStatus() == CustomerStatus.INACTIVE_CUSTOMER) {
            throw new IllegalArgumentException("停用客户不能提交客户接触记录");
        }
        CustomerContact contact = new CustomerContact(
                salesMember,
                customer,
                Instant.now(),
                request.communicationSummary());
        return CustomerContactResponse.from(customerContacts.save(contact));
    }

    public record CreateCustomerContactRequest(
            @NotBlank String customerId,
            @NotBlank String communicationSummary) {
    }

    public record CustomerContactResponse(
            String id,
            String customerId,
            String customerName,
            String salesMemberId,
            String salesMemberName,
            Instant contactTime,
            String communicationSummary) {
        public static CustomerContactResponse from(CustomerContact contact) {
            return new CustomerContactResponse(
                    contact.getId(),
                    contact.getCustomer().getId(),
                    contact.getCustomer().getName(),
                    contact.getSalesMember().getId(),
                    contact.getSalesMember().getName(),
                    contact.getContactTime(),
                    contact.getCommunicationSummary());
        }
    }
}
