package com.cms.backend.activity;

import com.cms.backend.security.CurrentAccount;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales/activity-calendar")
@PreAuthorize("hasRole('SALES_MEMBER')")
@Transactional(readOnly = true)
public class SalesActivityCalendarController {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final PlannedContactRepository plannedContacts;
    private final CustomerContactRepository customerContacts;

    public SalesActivityCalendarController(
            PlannedContactRepository plannedContacts,
            CustomerContactRepository customerContacts) {
        this.plannedContacts = plannedContacts;
        this.customerContacts = customerContacts;
    }

    @GetMapping("/today")
    public List<TodayPlannedContactResponse> getTodayPlannedContacts(@AuthenticationPrincipal CurrentAccount current) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        String salesMemberId = current.account().getId();
        Instant businessDayStart = today.atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant nextBusinessDayStart = today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        Map<String, Instant> lastContactTimes = customerContacts.findLatestContactSummariesBySalesMemberId(salesMemberId)
                .stream()
                .collect(Collectors.toMap(
                        CustomerContactRepository.CustomerContactSummary::getCustomerId,
                        CustomerContactRepository.CustomerContactSummary::getLastContactTime));

        return plannedContacts.findBySalesMemberIdAndPlannedDate(current.account().getId(), today).stream()
                .map(plannedContact -> TodayPlannedContact.from(
                        plannedContact,
                        customerContacts.existsBySalesMemberIdAndCustomerIdAndContactTimeGreaterThanEqualAndContactTimeLessThan(
                                salesMemberId,
                                plannedContact.getCustomer().getId(),
                                businessDayStart,
                                nextBusinessDayStart),
                        lastContactTimes.get(plannedContact.getCustomer().getId())))
                .sorted(Comparator
                        .comparing(TodayPlannedContact::registered)
                        .thenComparing(Comparator.comparingInt(TodayPlannedContact::attentionLevel).reversed())
                        .thenComparing(TodayPlannedContact::hasBeenContacted)
                        .thenComparing(TodayPlannedContact::lastContactTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(TodayPlannedContact::customerName))
                .map(TodayPlannedContact::toResponse)
                .toList();
    }

    private record TodayPlannedContact(
            String plannedContactId,
            String customerId,
            String customerName,
            int attentionLevel,
            LocalDate plannedDate,
            boolean registered,
            Instant lastContactTime) {
        static TodayPlannedContact from(
                PlannedContact plannedContact,
                boolean registered,
                Instant lastContactTime) {
            return new TodayPlannedContact(
                    plannedContact.getId(),
                    plannedContact.getCustomer().getId(),
                    plannedContact.getCustomer().getName(),
                    plannedContact.getCustomer().getAttentionLevel(),
                    plannedContact.getPlannedDate(),
                    registered,
                    lastContactTime);
        }

        boolean hasBeenContacted() {
            return lastContactTime != null;
        }

        TodayPlannedContactResponse toResponse() {
            return new TodayPlannedContactResponse(
                    plannedContactId,
                    customerId,
                    customerName,
                    attentionLevel,
                    plannedDate,
                    registered);
        }
    }

    public record TodayPlannedContactResponse(
            String plannedContactId,
            String customerId,
            String customerName,
            int attentionLevel,
            LocalDate plannedDate,
            boolean registered) {
    }
}
