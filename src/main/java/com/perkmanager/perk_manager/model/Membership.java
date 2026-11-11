package com.perkmanager.perk_manager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Membership entity
@Entity
@Table(name = "memberships")
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    //Required by JPA
    public Membership(){ }

    public Membership(String name, String description){
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    // Two memberships are the same row if they have the same (non-null) id
    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof Membership other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode(){
        return Membership.class.hashCode();
    }

    @Override
    public String toString(){
        return "Membership {id= " + id + ", name= ' " + name +"'}";
    }
}
