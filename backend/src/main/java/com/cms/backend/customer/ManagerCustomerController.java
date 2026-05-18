package com.cms.backend.customer;

import com.cms.backend.account.UserAccount;
import com.cms.backend.account.UserAccountRepository;
import com.cms.backend.activity.CustomerContact;
import com.cms.backend.activity.CustomerContactRepository;
import com.cms.backend.activity.PlannedContact;
import com.cms.backend.activity.PlannedContactRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/api/manager/customers")
@Transactional(readOnly = true)
public class ManagerCustomerController {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final CustomerRepository customers;
    private final UserAccountRepository accounts;
    private final PlannedContactRepository plannedContacts;
    private final CustomerContactRepository customerContacts;

    public ManagerCustomerController(
            CustomerRepository customers,
            UserAccountRepository accounts,
            PlannedContactRepository plannedContacts,
            CustomerContactRepository customerContacts) {
        this.customers = customers;
        this.accounts = accounts;
        this.plannedContacts = plannedContacts;
        this.customerContacts = customerContacts;
    }

    @GetMapping
    public List<ManagerCustomerResponse> listCustomers(
            @RequestParam(required = false, defaultValue = "") String name,
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) Integer attentionLevel) {
        return customers.findAllByOrderByAttentionLevelDescNameAsc().stream()
                .filter(customer -> name.isBlank() || customer.getName().contains(name))
                .filter(customer -> ownerId == null || customer.getOwner().getId().equals(ownerId))
                .filter(customer -> status == null || customer.getStatus() == status)
                .filter(customer -> attentionLevel == null || customer.getAttentionLevel() == attentionLevel)
                .map(ManagerCustomerResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ManagerCustomerDetailResponse getCustomer(@PathVariable String id) {
        Customer customer = findCustomer(id);
        return ManagerCustomerDetailResponse.from(
                customer,
                plannedContacts.findByCustomerIdAndPlannedDateAfterOrderByPlannedDateAsc(id, LocalDate.now(BUSINESS_ZONE)),
                customerContacts.findByCustomerIdOrderByContactTimeDesc(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ManagerCustomerResponse createCustomer(@Valid @RequestBody CreateManagerCustomerRequest request) {
        if (customers.existsByName(request.name())) {
            throw new IllegalArgumentException("客户名称已存在");
        }
        UserAccount owner = findActiveSalesMember(request.ownerId());
        Customer customer = new Customer(request.name(), request.status(), owner, request.agreementSigningDate());
        return ManagerCustomerResponse.from(customers.save(customer));
    }

    @PatchMapping("/{id}")
    @Transactional
    public ManagerCustomerResponse updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody UpdateManagerCustomerRequest request) {
        Customer customer = findCustomer(id);
        if (customers.existsByNameAndIdNot(request.name(), id)) {
            throw new IllegalArgumentException("客户名称已存在");
        }
        UserAccount owner = findActiveSalesMember(request.ownerId());
        customer.rename(request.name());
        customer.changeOwner(owner);
        customer.changeAttentionLevel(request.attentionLevel());
        return ManagerCustomerResponse.from(customers.save(customer));
    }

    @PostMapping("/{id}/deactivate")
    @Transactional
    public ManagerCustomerResponse deactivateCustomer(@PathVariable String id) {
        Customer customer = findCustomer(id);
        customer.deactivate();
        return ManagerCustomerResponse.from(customers.save(customer));
    }

    @PostMapping("/{id}/restore")
    @Transactional
    public ManagerCustomerResponse restoreCustomer(@PathVariable String id) {
        Customer customer = findCustomer(id);
        customer.restore();
        return ManagerCustomerResponse.from(customers.save(customer));
    }

    private Customer findCustomer(String id) {
        return customers.findById(id).orElseThrow(() -> new IllegalArgumentException("客户不存在"));
    }

    private UserAccount findActiveSalesMember(String ownerId) {
        UserAccount owner = accounts.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("负责人不存在"));
        if (!owner.isActive() || !owner.isSalesMember()) {
            throw new IllegalArgumentException("负责人必须是启用的销售成员");
        }
        return owner;
    }

    public record CreateManagerCustomerRequest(
            @NotBlank String name,
            CustomerStatus status,
            @NotBlank String ownerId,
            LocalDate agreementSigningDate) {
    }

    public record UpdateManagerCustomerRequest(
            @NotBlank String name,
            @NotBlank String ownerId,
            int attentionLevel) {
    }

    public record ManagerCustomerResponse(
            String id,
            String name,
            CustomerStatus status,
            int attentionLevel,
            OwnerResponse owner,
            LocalDate agreementSigningDate) {
        public static ManagerCustomerResponse from(Customer customer) {
            return new ManagerCustomerResponse(
                    customer.getId(),
                    customer.getName(),
                    customer.getStatus(),
                    customer.getAttentionLevel(),
                    OwnerResponse.from(customer.getOwner()),
                    customer.getAgreementSigningDate());
        }
    }

    public record OwnerResponse(String id, String name) {
        public static OwnerResponse from(UserAccount owner) {
            return new OwnerResponse(owner.getId(), owner.getName());
        }
    }

    public record ManagerCustomerDetailResponse(
            String id,
            String name,
            CustomerStatus status,
            int attentionLevel,
            OwnerResponse owner,
            LocalDate agreementSigningDate,
            List<PlannedContactResponse> futurePlannedContacts,
            List<CustomerContactResponse> customerContacts) {
        public static ManagerCustomerDetailResponse from(
                Customer customer,
                List<PlannedContact> plannedContacts,
                List<CustomerContact> customerContacts) {
            return new ManagerCustomerDetailResponse(
                    customer.getId(),
                    customer.getName(),
                    customer.getStatus(),
                    customer.getAttentionLevel(),
                    OwnerResponse.from(customer.getOwner()),
                    customer.getAgreementSigningDate(),
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
