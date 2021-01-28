package com.dojomanager.data.entities.dojo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import com.dojomanager.data.entities.AbstractEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
public class Dojo extends AbstractEntity{

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private DojoOwner owner;

    @Getter @Setter
    private String name;

    @Getter @Setter
    private String website;

    // TODO
    // private Member leadInstructor;
    // private Address address;
}
