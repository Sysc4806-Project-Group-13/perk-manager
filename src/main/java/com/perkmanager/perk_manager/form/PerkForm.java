package com.perkmanager.perk_manager.form;

import com.perkmanager.perk_manager.model.DiscountType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PerkForm {

    @NotBlank(message = "Enter a title for the perk.")
    @Size(max = 100, message = "The title cannot be longer than 100 characters.")
    private String title;

    @Size(max = 2000, message = "The description cannot be longer than 2,000 characters.")
    private String description;

    @NotBlank(message = "Enter the product or service this perk applies to.")
    @Size(max = 100, message = "The product or service cannot be longer than 100 characters.")
    private String productName;

    @NotNull(message = "Choose a discount type.")
    private DiscountType discountType;

    private BigDecimal discountValue;

    @Size(max = 100, message = "The geographic restriction cannot be longer than 100 characters.")
    private String geographicArea;

    @FutureOrPresent(message = "The expiry date cannot be in the past.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @NotNull(message = "Choose an existing membership.")
    private Long membershipId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public String getGeographicArea() {
        return geographicArea;
    }

    public void setGeographicArea(String geographicArea) {
        this.geographicArea = geographicArea;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Long membershipId) {
        this.membershipId = membershipId;
    }
}