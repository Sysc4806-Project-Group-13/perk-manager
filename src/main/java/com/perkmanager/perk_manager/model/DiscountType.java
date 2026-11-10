package com.perkmanager.perk_manager.model;

// Type of benefit a perk offers. Names must match the values allowed by t
// chk_perks_discount_type constraints in V1
public enum DiscountType {
    PERCENTAGE,     //discountValue = percent off (10 -> 10%)
    FIXED_AMOUNT,   // discountValue = dollars off (10 -> $10)
    FREE_ITEM,      // discountValue usually null
    POINTS_REDEMPTION,  //discountValue = points required (10000 -> 10,000 points)
    OTHER
}
