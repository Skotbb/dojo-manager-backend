package com.dojomanager.data.dto.dojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankLevelDto {
    private Long id;
    private Long rankSettingId;
    private int sortOrder;
    private int number;
    private int hoursRequired;
    private String beltColor;
    private String stripeColor;
    private Integer stripeCount;
}
