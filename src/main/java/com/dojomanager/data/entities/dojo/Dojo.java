package com.dojomanager.data.entities.dojo;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.Data;

@Entity
@Data
public class Dojo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(targetEntity = DojoOwner.class, fetch = FetchType.LAZY)
    // @JoinColumn(name="ownerId")
    private DojoOwner owner;

    private String name;
    private String website;

    // TODO
    // private Member leadInstructor;
    // private Address address;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dojo )) return false;
        return id != null && id.equals(((Dojo) o).getId());
    }
}
