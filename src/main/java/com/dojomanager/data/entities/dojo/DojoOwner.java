package com.dojomanager.data.entities.dojo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

import com.dojomanager.data.entities.AbstractEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
public class DojoOwner extends AbstractEntity{
    @JsonIgnore
    @OneToMany(cascade = CascadeType.PERSIST, orphanRemoval = true, mappedBy = "owner")
    // @JoinColumn(name = "owner_id")
    private Set<Dojo> managedDojos = new HashSet<>();
    
    @Getter @Setter
    private String firstName;

    @Getter @Setter
    private String lastName;

    @Getter @Setter
    @Column(unique = true)
    private String email;

    @Getter @Setter
    private String password;

    public DojoOwner(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
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
