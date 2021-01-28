package com.dojomanager.data.entities.rank;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import com.dojomanager.data.entities.AbstractEntity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Entity
public class BeltColor extends AbstractEntity{

    @Getter @Setter
    private String color;
    
    @Getter @Setter
    private String stripeColor;

    @Getter
    private int stripeCount;

    @Getter @Setter
    @ManyToOne(optional = false)
    private RankLevel rankLevel;

    public void setStripeCount(Integer count) {
        this.stripeCount = count != null ? count : 0;
    }

    public BeltColor(String color, String stripeColor, Integer stripeCount, RankLevel rankLevel) {
        this.color = color;
        this.stripeColor = stripeColor;
        this.setStripeCount(stripeCount);
        this.rankLevel = rankLevel;
    }    
}
