package com.perkmanager.perk_manager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

// Perk Entity
@Entity
@Table(name = "perks")
public class Perk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    //Product or service the perk applies to. (ex. "Movie Tickets")
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    //Stored as enum's name ('PERCENTAGE', 'FIXED_AMOUNT', etc...)
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 100)
    private DiscountType discountType;

    // Meaning depends on the discountType (percent, dollars, points), null if not applicable.
    // Up to 10 digits, 2 of which can be after the decimal point
    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    // Null means that the perk can be used anywhere
    @Column(name = "geographic_area", length = 100)
    private String geographicArea;

    // Null means the perk never expires
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Many perks belong to one membership -> perks is the owning side
    // This field writes the membership_id foreign key column
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "membership_id", nullable = false)
    private Membership membership;

    // Required by JPA
    public Perk() { }

    // Constructor with fields required by the database (columns that are not NULL)
    public Perk(String title, String productName, DiscountType discountType, Membership membership){
        this.title = title;
        this.productName = productName;
        this.discountType = discountType;
        this.membership = membership;
    }

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public DiscountType getDiscountType() { return discountType;}
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }

    public String getGeographicArea() { return geographicArea; }
    public void setGeographicArea(String geographicArea) { this.geographicArea = geographicArea; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public Membership getMembership() { return membership; }
    public void setMembership(Membership membership) { this.membership = membership; }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof Perk other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode(){
        return Perk.class.hashCode();
    }

    // Deliberately does not print membership it is assigned to
    @Override
    public String toString(){
        return "Perk {id= " + id + ", title= '" + title + "', product= '" + productName + "', type= " + discountType
                + ", expiryDate= " + expiryDate + "}";
    }
}
