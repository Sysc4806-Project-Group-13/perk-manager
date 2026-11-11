package com.perkmanager.perk_manager.repository;

import com.perkmanager.perk_manager.model.DiscountType;
import com.perkmanager.perk_manager.model.Perk;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PerkRepository extends JpaRepository<Perk, Long> {
    // Retrieves all the perks of a certain membership.
    // @EntityGraph loads the membership in the same query
    @EntityGraph(attributePaths = "membership")
    List<Perk> findByMembershipId(Long membershipId);

    // Retrieves all the perks belonging to any membership in a user's profile
    @EntityGraph(attributePaths = "membership")
    List<Perk> findByMembershipIdIn(Collection<Long> membershipIds);

    // Retrieve all perks that have a particular discount type (ex. every PERCENTAGE perk)
    @EntityGraph(attributePaths = "membership")
    List<Perk> findByDiscountType(DiscountType discountType);

    // Retrieves perks that haven't expired yet (no expiry date, or expiry is today / in the future)
    // the soonest expiry is first, and perks that never expire are last
    @Query("""
            select p from Perk p
            join fetch p.membership
            where p.expiryDate is null or p.expiryDate >= :today
            order by p.expiryDate asc nulls last, p.id asc
            """)
    List<Perk> findActive(@Param("today") LocalDate today);

    // Retrieves active perks that are usable in the given area:
    // perks with no restriction, or restricted to that area
    @Query("""
            select p from Perk p
            join fetch p.membership
            where (p.expiryDate is null or p.expiryDate >= :today)
                and (p.geographicArea is null or lower(p.geographicArea) = lower(:area))
            order by p.expiryDate asc nulls last, p.id asc
            """)
    List<Perk> findActiveInArea(@Param("today") LocalDate today, @Param("area") String area);

    // Retrieves active perks where their product, title, description or membership name contains
    // the keyword (case-insensitive). "" (not null) is passed to match everything.
    @Query("""
            select p from Perk p
            join fetch p.membership m
            where (p.expiryDate is null or p.expiryDate >= :today)
                and (lower(p.productName) like lower(concat('%', :keyword, '%'))
                    or lower(p.title) like lower(concat('%', :keyword, '%'))
                    or lower(p.description) like lower(concat('%', :keyword, '%'))
                    or lower(m.name) like lower(concat('%', :keyword, '%')))
            order by p.expiryDate asc nulls last, p.id asc
            """)
    List<Perk> searchActive(@Param("keyword") String keyword, @Param("today") LocalDate today);

    // Same search as above, but retrieval is restricted to memberships specifically in a user's profile
    // must not be passed an empty collection (a user that has no memberships will have no matches)
    // Basically the Logged-In User version
    @Query("""
            select p from Perk p
            join fetch p.membership m
            where m.id in :membershipIds
                and (p.expiryDate is null or p.expiryDate >= :today)
                and (lower(p.productName) like lower(concat('%', :keyword, '%'))
                    or lower(p.title) like lower(concat('%', :keyword, '%'))
                    or lower(p.description) like lower(concat('%', :keyword, '%')))
            order by p.expiryDate asc nulls last, p.id asc
            """)
    List<Perk> searchActiveForMemberships(@Param("keyword") String keyword,
                                          @Param("today") LocalDate today,
                                          @Param("membershipIds") Collection<Long> membershipIds);

    // Will be updated with sort by votes when the schema has vote columns added.

}
