package com.dojomanager.data.dto.dojo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankDto {
    private Long id;
    private String name;
    private List<RankLevelDto> levels;
}
