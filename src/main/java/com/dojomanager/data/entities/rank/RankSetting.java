package com.dojomanager.data.entities.rank;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.dojomanager.data.entities.AbstractEntity;
import com.dojomanager.data.entities.dojo.Dojo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
public class RankSetting extends AbstractEntity{
    @Getter @Setter
    @ManyToOne
    private Dojo dojo;

    @Getter @Setter
    @ManyToOne
    private RankName rankName;

    @Getter @Setter
    @ManyToOne
    private RankLevel rankLevel;
    
    public RankSetting(Dojo dojo, RankName rankName, RankLevel rankLevel) {
        this.dojo = dojo;
        this.rankName = rankName;
        this.rankLevel = rankLevel;
    }
}
