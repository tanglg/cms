package com.cms.backend.customer;

import com.cms.backend.account.UserAccount;
import com.cms.backend.activity.CustomerContact;
import com.cms.backend.activity.CustomerContactRepository;
import com.cms.backend.activity.PlannedContact;
import com.cms.backend.activity.PlannedContactRepository;
import com.cms.backend.security.CurrentAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales/customers")
@PreAuthorize("hasRole('SALES_MEMBER')")
public class SalesCustomerController {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final CustomerRepository customers;
    private final PlannedContactRepository plannedContacts;
    private final CustomerContactRepository customerContacts;

    public SalesCustomerController(
            CustomerRepository customers,
            PlannedContactRepository plannedContacts,
            CustomerContactRepository customerContacts) {
        this.customers = customers;
        this.plannedContacts = plannedContacts;
        this.customerContacts = customerContacts;
    }

    @GetMapping
    public List<CustomerResponse> listCustomers(
            @AuthenticationPrincipal CurrentAccount current,
            @RequestParam(required = false, defaultValue = "") String name) {
        UserAccount owner = current.account();
        List<Customer> matches = name.isBlank()
                ? customers.findByOwnerIdOrderByAttentionLevelDescNameAsc(owner.getId())
                : customers.findByOwnerIdAndNameContainingOrderByAttentionLevelDescNameAsc(owner.getId(), name);
        return matches.stream()
                .map(customer -> CustomerResponse.from(customer, owner))
                .toList();
    }

    @GetMapping("/{id}")
    public SalesCustomerDetailResponse getCustomerDetail(
            @AuthenticationPrincipal CurrentAccount current,
            @PathVariable String id) {
        Customer customer = customers.findByIdAndOwnerId(id, current.account().getId())
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
        return SalesCustomerDetailResponse.from(
                customer,
                plannedContacts.findByCustomerIdAndPlannedDateAfterOrderByPlannedDateAsc(id, LocalDate.now(BUSINESS_ZONE)),
                customerContacts.findByCustomerIdAndSalesMemberIdOrderByContactTimeDesc(id, current.account().getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(
            @AuthenticationPrincipal CurrentAccount current,
            @Valid @RequestBody CreateCustomerRequest request) {
        if (customers.existsByName(request.name())) {
            throw new IllegalArgumentException("客户名称已存在");
        }
        UserAccount owner = current.account();
        Customer customer = new Customer(request.name(), owner);
        return CustomerResponse.from(customers.save(customer), owner);
    }

    @PatchMapping("/{id}/attention-level")
    public CustomerResponse updateAttentionLevel(
            @AuthenticationPrincipal CurrentAccount current,
            @PathVariable String id,
            @Valid @RequestBody UpdateAttentionLevelRequest request) {
        UserAccount owner = current.account();
        Customer customer = customers.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
        customer.changeAttentionLevel(request.attentionLevel());
        return CustomerResponse.from(customers.save(customer), owner);
    }

    public record CreateCustomerRequest(@NotBlank String name) {
    }

    public record UpdateAttentionLevelRequest(int attentionLevel) {
    }

    public record CustomerResponse(
            String id,
            String name,
            CustomerStatus status,
            int attentionLevel,
            OwnerResponse owner) {
        public static CustomerResponse from(Customer customer, UserAccount owner) {
            return new CustomerResponse(
                    customer.getId(),
                    customer.getName(),
                    customer.getStatus(),
                    customer.getAttentionLevel(),
                    OwnerResponse.from(owner));
        }
    }

    public record OwnerResponse(String id, String name) {
        public static OwnerResponse from(UserAccount owner) {
            return new OwnerResponse(owner.getId(), owner.getName());
        }
    }

    public record SalesCustomerDetailResponse(
            String id,
            String name,
            CustomerStatus status,
            int attentionLevel,
            List<PlannedContactResponse> futurePlannedContacts,
            List<CustomerContactResponse> customerContacts) {
        public static SalesCustomerDetailResponse from(
                Customer customer,
                List<PlannedContact> plannedContacts,
                List<CustomerContact> customerContacts) {
            return new SalesCustomerDetailResponse(
                    customer.getId(),
                    customer.getName(),
                    customer.getStatus(),
                    customer.getAttentionLevel(),
                    plannedContacts.stream().map(PlannedContactResponse::from).toList(),
                    customerContacts.stream().map(CustomerContactResponse::from).toList());
        }
    }

    public record PlannedContactResponse(String id, String customerId, String customerName, LocalDate plannedDate) {
        public static PlannedContactResponse from(PlannedContact plannedContact) {
            return new PlannedContactResponse(
                    plannedContact.getId(),
                    plannedContact.getCustomer().getId(),
                    plannedContact.getCustomer().getName(),
                    plannedContact.getPlannedDate());
        }
    }

    public record CustomerContactResponse(
            String id,
            String customerId,
            String customerName,
            String salesMemberId,
            String salesMemberName,
            java.time.Instant contactTime,
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
