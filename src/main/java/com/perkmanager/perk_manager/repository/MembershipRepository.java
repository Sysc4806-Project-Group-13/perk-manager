package com.perkmanager.perk_manager.repository;

import com.perkmanager.perk_manager.model.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    // For dropdowns & profile page: every membership, alphabetical
    List<Membership> findAllByOrderByNameAsc();

    // Exact-name lookup
    Optional<Membership> findByName(String name);

    // Partial, case-insensitive name search, e.g. "visa" finds "Visa Infinite"
    List<Membership> findByNameContainingIgnoreCase(String name);

    // Lets the service reject duplicates (including "visa" vs "Visa") before inserting
    boolean existsByNameIgnoreCase(String name);
}
