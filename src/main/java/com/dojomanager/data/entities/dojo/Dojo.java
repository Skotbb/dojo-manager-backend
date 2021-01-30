package com.dojomanager.data.entities.dojo;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.dojomanager.data.entities.AbstractEntity;
import com.dojomanager.data.entities.people.Address;
import com.dojomanager.data.entities.people.Student;
import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @Getter @Setter
    @ManyToOne
    private Address address;

    @Getter @Setter
    @ManyToOne
    private Student leadInstructor;

    @Getter
    @OneToMany(mappedBy = "dojo", orphanRemoval = true, cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<Student> students = new HashSet<>();

    public Dojo(String name, String website) {
        this.name = name;
        this.website = website;
    }

    public Dojo(String name, String website, Address address) {
        this.name = name;
        this.website = website;
        this.address = address;
    }

    public void addStudent(Student student) {
        students.add(student);
        student.setDojo(this);
    }
}
