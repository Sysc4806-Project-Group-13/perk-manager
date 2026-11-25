package com.perkmanager.perk_manager.search;

import com.perkmanager.perk_manager.model.Membership;
import java.util.List;

// The choices shown in the page's dropdowns and the product autocomplete
public class PerkFilterOptions {
    private final List<Membership> memberships;
    private final List<String> areas;
    private final List<String> productNames;

    public PerkFilterOptions(List<Membership> memberships, List<String> areas, List<String> productNames){
        this.memberships = memberships;
        this.areas = areas;
        this.productNames = productNames;
    }

    // Uses getters rather than being a record, to match PerkRow
    public List<Membership> getMemberships() { return memberships; }
    public List<String> getAreas() { return areas; }
    public List<String> getProductNames() { return productNames; }
}
