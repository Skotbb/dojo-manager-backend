package com.dojomanager.data.entities.rank;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.dojomanager.data.entities.AbstractEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
public class RankLevel extends AbstractEntity{
    
    @Getter @Setter
    private int sortOrder;

    @Getter @Setter
    private int number;

    @Getter @Setter
    private int hoursRequired;

    @Getter @Setter
    @ManyToOne(optional = false)
    private RankName rankName;


    public RankLevel(int sortOrder, int number, int hoursRequired, RankName rankName) {
        this.sortOrder = sortOrder;
        this.number = number;
        this.hoursRequired = hoursRequired;
        this.rankName = rankName;
    }
}
