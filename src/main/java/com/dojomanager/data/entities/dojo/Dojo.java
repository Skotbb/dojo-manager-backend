package com.dojomanager.data.entities.dojo;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
