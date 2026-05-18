package com.cms.backend.activity;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerContactRepository extends JpaRepository<CustomerContact, String> {
    boolean existsBySalesMemberIdAndCustomerIdAndContactTimeGreaterThanEqualAndContactTimeLessThan(
            String salesMemberId,
            String customerId,
            Instant startInclusive,
            Instant endExclusive);

    @Query("""
            select c.customer.id as customerId, max(c.contactTime) as lastContactTime
            from CustomerContact c
            where c.salesMember.id = :salesMemberId
            group by c.customer.id
            """)
    List<CustomerContactSummary> findLatestContactSummariesBySalesMemberId(String salesMemberId);

    @EntityGraph(attributePaths = {"customer", "salesMember"})
    List<CustomerContact> findByCustomerIdAndSalesMemberIdOrderByContactTimeDesc(String customerId, String salesMemberId);

    @EntityGraph(attributePaths = {"customer", "salesMember"})
    List<CustomerContact> findByCustomerIdOrderByContactTimeDesc(String customerId);

    interface CustomerContactSummary {
        String getCustomerId();

        Instant getLastContactTime();
    }
}
