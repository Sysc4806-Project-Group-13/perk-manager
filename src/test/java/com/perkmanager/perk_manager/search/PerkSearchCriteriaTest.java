package com.perkmanager.perk_manager.search;

import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PerkSearchCriteriaTest {

    @Test
    void nullsAndWhitespaceBecomeEmptyStrings() {
        PerkSearchCriteria c = new PerkSearchCriteria("  tea  ", false, null, null, null, null, null);
        assertEquals("tea", c.keyword());
        assertEquals("", c.area());
    }

    @Test
    void nonPositiveExpiryWindowIsIgnored() {
        assertNull(new PerkSearchCriteria("", false, "", null, null, 0, null).expiringWithinDays());
        assertNull(new PerkSearchCriteria("", false, "", null, null, -5, null).expiringWithinDays());
    }

    @Test
    void hasFiltersIsFalseForAnEmptySearchAndTrueOnceAnythingIsSet() {
        assertFalse(PerkSearchCriteria.of(null, null, null).hasFilters());
        assertTrue(PerkSearchCriteria.of("tea", null, null).hasFilters());
        assertTrue(PerkSearchCriteria.of(null, null, Set.of(1L)).hasFilters());
    }
}