package com.dojomanager.data.entities.dojo;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.dojomanager.data.entities.AbstractEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
@Table(name = "dojo")
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


    public Dojo(String name, String website) {
        this.name = name;
        this.website = website;
    }
}
