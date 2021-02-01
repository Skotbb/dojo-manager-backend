package com.dojomanager.data.entities.rank;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import com.dojomanager.data.entities.AbstractEntity;
import com.dojomanager.data.entities.people.Student;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
public class StudentRank extends AbstractEntity{
    @Getter @Setter
    private double hoursInRank;

    @Getter @Setter
    @ManyToOne
    private RankSetting rankSetting;

    @Getter @Setter
    @OneToOne(mappedBy = "rank")
    private Student student;

    public StudentRank(double hoursInRank, RankSetting rankSetting) {
        this.hoursInRank = hoursInRank;
        this.rankSetting = rankSetting;
    }
}
