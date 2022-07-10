package com.dojomanager.data.dto.dojo;

import lombok.Data;

@Data
public class RankSettingDto {
    private int sortOrder;
    private int number;
    private double hoursRequired;
    private String beltColor;
    private String stripeColor;
    private int stripeCount;
    private String rankName;
}
