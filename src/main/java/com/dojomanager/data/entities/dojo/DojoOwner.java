package com.dojomanager.data.entities.dojo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.dojomanager.data.entities.people.Person;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Table(name = "dojo_owner")
public class DojoOwner extends Person{
    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owner")
    private Set<Dojo> managedDojos = new HashSet<>();

    @Getter @Setter
    private String password;

    public DojoOwner(String firstName, String lastName, String email, String password) {
        super(firstName, lastName, email);
        this.password = password;
    }

    public Set<Dojo> getManagedDojos() {
        return Collections.unmodifiableSet(this.managedDojos);
    }

    public void addDojo(Dojo dojo) {
        managedDojos.add(dojo);
        dojo.setOwner(this);
    }

    public void removeDojo(Dojo dojo) {
        managedDojos.remove(dojo);
        dojo.setOwner(null);
    }
}
