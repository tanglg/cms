package com.cms.backend.activity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlannedContactRepository extends JpaRepository<PlannedContact, String> {
    boolean existsBySalesMemberIdAndCustomerIdAndPlannedDate(String salesMemberId, String customerId, LocalDate plannedDate);

    boolean existsBySalesMemberIdAndCustomerIdAndPlannedDateAndIdNot(
            String salesMemberId, String customerId, LocalDate plannedDate, String id);

    @EntityGraph(attributePaths = "customer")
    Optional<PlannedContact> findByIdAndSalesMemberId(String id, String salesMemberId);

    @EntityGraph(attributePaths = "customer")
    List<PlannedContact> findBySalesMemberIdAndPlannedDate(String salesMemberId, LocalDate plannedDate);

    @EntityGraph(attributePaths = "customer")
    List<PlannedContact> findByCustomerIdAndPlannedDateAfterOrderByPlannedDateAsc(String customerId, LocalDate date);
}
