package com.cms.backend.activity;

import com.cms.backend.account.UserAccount;
import com.cms.backend.customer.Customer;
import com.cms.backend.customer.CustomerRepository;
import com.cms.backend.customer.CustomerStatus;
import com.cms.backend.security.CurrentAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales/planned-contacts")
@PreAuthorize("hasRole('SALES_MEMBER')")
@Transactional
public class SalesPlannedContactController {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final CustomerRepository customers;
    private final PlannedContactRepository plannedContacts;

    public SalesPlannedContactController(CustomerRepository customers, PlannedContactRepository plannedContacts) {
        this.customers = customers;
        this.plannedContacts = plannedContacts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlannedContactResponse createPlannedContact(
            @AuthenticationPrincipal CurrentAccount current,
            @Valid @RequestBody CreatePlannedContactRequest request) {
        UserAccount salesMember = current.account();
        Customer customer = customers.findByIdAndOwnerId(request.customerId(), salesMember.getId())
                .orElseThrow(() -> new IllegalArgumentException("客户不存在"));
        if (customer.getStatus() == CustomerStatus.INACTIVE_CUSTOMER) {
            throw new IllegalArgumentException("停用客户不能创建计划接触");
        }
        if (!request.plannedDate().isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw new IllegalArgumentException("计划日期必须是未来日期");
        }
        if (plannedContacts.existsBySalesMemberIdAndCustomerIdAndPlannedDate(
                salesMember.getId(), customer.getId(), request.plannedDate())) {
            throw new IllegalArgumentException("同一客户同一天已有计划接触");
        }
        PlannedContact plannedContact = new PlannedContact(salesMember, customer, request.plannedDate());
        return PlannedContactResponse.from(plannedContacts.save(plannedContact));
    }

    @PatchMapping("/{id}")
    public PlannedContactResponse updatePlannedContact(
            @AuthenticationPrincipal CurrentAccount current,
            @PathVariable String id,
            @Valid @RequestBody UpdatePlannedContactRequest request) {
        UserAccount salesMember = current.account();
        PlannedContact plannedContact = plannedContacts.findByIdAndSalesMemberId(id, salesMember.getId())
                .orElseThrow(() -> new IllegalArgumentException("计划接触不存在"));
        if (!request.plannedDate().isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw new IllegalArgumentException("计划日期必须是未来日期");
        }
        if (plannedContacts.existsBySalesMemberIdAndCustomerIdAndPlannedDateAndIdNot(
                salesMember.getId(), plannedContact.getCustomer().getId(), request.plannedDate(), id)) {
            throw new IllegalArgumentException("同一客户同一天已有计划接触");
        }
        plannedContact.changePlannedDate(request.plannedDate());
        return PlannedContactResponse.from(plannedContacts.save(plannedContact));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlannedContact(
            @AuthenticationPrincipal CurrentAccount current,
            @PathVariable String id) {
        PlannedContact plannedContact = plannedContacts.findByIdAndSalesMemberId(id, current.account().getId())
                .orElseThrow(() -> new IllegalArgumentException("计划接触不存在"));
        if (!plannedContact.getPlannedDate().isAfter(LocalDate.now(BUSINESS_ZONE))) {
            throw new IllegalArgumentException("今天和过去的计划不能手动删除");
        }
        plannedContacts.delete(plannedContact);
    }

    public record CreatePlannedContactRequest(@NotBlank String customerId, @NotNull LocalDate plannedDate) {
    }

    public record UpdatePlannedContactRequest(@NotNull LocalDate plannedDate) {
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
}
