package com.dojomanager.data.dto.dojo;

import java.util.List;

import lombok.Data;

@Data
public class NewDojoDto {
    private DojoDto dojo;
    private List<RankSettingDto> rankSettings;
}
