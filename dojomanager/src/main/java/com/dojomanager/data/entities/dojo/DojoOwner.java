package com.dojomanager.data.entities.dojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Data
public class DojoOwner {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    // @Getter @Setter
    private Integer id;

    @OneToMany(targetEntity=Dojo.class, cascade = CascadeType.REMOVE, fetch = FetchType.LAZY, mappedBy = "owner")
    private List<Dojo> managedDojos = new ArrayList<>();
    
    // @Getter @Setter
    private String firstName;
    // @Getter @Setter
    private String lastName;
    // @Getter @Setter
    private String email;
    // @Getter @Setter
    private String password;

    // public void setManagedDojos(Set<Dojo> dojos) {
    //     this.managedDojos = dojos;
    // }
    // public Set<Dojo> getManagedDojos() {
    //     return this.managedDojos;
    // }

    // public void addDojo(Dojo dojo) {
    //     managedDojos.add(dojo);
    //     dojo.setOwner(this);
    // }

    // public void removeDojo(Dojo dojo) {
    //     managedDojos.remove(dojo);
    //     dojo.setOwner(null);
    // }
}
