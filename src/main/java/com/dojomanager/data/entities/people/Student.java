package com.dojomanager.data.entities.people;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

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
