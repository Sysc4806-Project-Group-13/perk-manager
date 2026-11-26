package com.perkmanager.perk_manager.search;

import com.perkmanager.perk_manager.model.DiscountType;
import java.util.Set;

// Everything the /perks page can filter on. Blank/null fields mean "don't filter on this".
public record PerkSearchCriteria (
        String keyword,             // free text, "" = match everything
        boolean productOnly,        // true = compare keyword with product/service name only
        String area,                // "" = any area
        Long membershipId,          // null = any membership
        DiscountType discountType,  // null = any benefit type
        Integer expiringWithinDays, // null = any expiry date
        Set<Long> onlyMembershipIds // all memberships (the "only mine" filter)
){
    // Constructor normalizes the values every time a record is created
    public PerkSearchCriteria{
        keyword = keyword == null ? "" : keyword.trim();
        area = area == null ? "" : area.trim();
        if (expiringWithinDays != null && expiringWithinDays <= 0){
            expiringWithinDays = null;
        }
    }

    // The original keyword + area + membership search
    public static PerkSearchCriteria of(String keyword, String area, Set<Long> onlyMembershipIds){
        return new PerkSearchCriteria(keyword, false, area, null, null, null, onlyMembershipIds);
    }

    // True if anything narrows the results (the page uses this to show "Clear")
    public boolean hasFilters(){
        return !keyword.isEmpty() || !area.isEmpty() || membershipId != null
                || discountType != null || expiringWithinDays != null || onlyMembershipIds != null;
    }
}
