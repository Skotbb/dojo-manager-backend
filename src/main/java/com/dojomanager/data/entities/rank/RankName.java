package com.dojomanager.data.entities.rank;

import javax.persistence.Entity;

import com.dojomanager.data.entities.AbstractEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@NoArgsConstructor
public class RankName extends AbstractEntity{
    
    @Getter @Setter
    private String name;


    public RankName(String name) {
        this.name = name;
    }
}
