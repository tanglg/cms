package com.cms.backend.customer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);

    List<Customer> findAllByOrderByAttentionLevelDescNameAsc();

    List<Customer> findByOwnerIdOrderByAttentionLevelDescNameAsc(String ownerId);

    List<Customer> findByOwnerIdAndNameContainingOrderByAttentionLevelDescNameAsc(String ownerId, String name);

    Optional<Customer> findByIdAndOwnerId(String id, String ownerId);
}
