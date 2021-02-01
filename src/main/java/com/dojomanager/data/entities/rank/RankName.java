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
public class RankName extends AbstractEntity{
    
    @Getter @Setter
    private String name;

    @Setter @Getter
    @ManyToOne(optional = false)
    private Dojo dojo;


    public RankName(String name, Dojo dojo) {
        this.name = name;
        this.dojo = dojo;
    }
}
