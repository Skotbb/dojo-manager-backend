package com.dojomanager.data.entities.people;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.rank.StudentRank;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
public class Student extends Person{
    
    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dojo_id", nullable = false, updatable = false)
    private Dojo dojo;

    @Getter @Setter
    @OneToOne
    @JoinColumn(name = "rank_id")
    private StudentRank rank;

    public Student(Dojo dojo, StudentRank rank) {
        this.dojo = dojo;
        this.rank = rank;
    }
}
