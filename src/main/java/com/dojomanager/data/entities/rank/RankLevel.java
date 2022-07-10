package com.dojomanager.data.entities.rank;

import javax.persistence.Entity;

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
    private double hoursRequired;

    @Getter @Setter
    private String beltColor;
    
    @Getter @Setter
    private String stripeColor;

    @Getter
    private int stripeCount;

    public void setStripeCount(Integer count) {
        this.stripeCount = count != null ? count : 0;
    }

    public RankLevel(int sortOrder, int number, double hoursRequired, String beltColor, String stripeColor, int stripeCount) {
        this.sortOrder = sortOrder;
        this.number = number;
        this.hoursRequired = hoursRequired;
        this.beltColor = beltColor;
        this.stripeColor = stripeColor;
        this.stripeCount = stripeCount;
    }
}
