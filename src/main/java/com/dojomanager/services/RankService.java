package com.dojomanager.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.dojomanager.data.dto.dojo.RankSettingDto;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.rank.StudentRank;
import com.dojomanager.data.entities.rank.RankLevel;
import com.dojomanager.data.entities.rank.RankName;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.data.repositories.rank.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class RankService {

    @Autowired
    RankNameRepository rankNameRepo;
    @Autowired
    RankLevelRepository rankLevelRepo;
    @Autowired
    RankSettingRepository settingRepo;
    @Autowired
    RankRepository rankRepo;

    public RankName saveRankName(RankName rank) {
        Optional<RankName> existingRankName = rankNameRepo.findByName(rank.getName());
        if (existingRankName.isPresent()) {
            return existingRankName.get();
        }
        return rankNameRepo.save(rank);
    }

    public RankLevel saveRankLevel(RankLevel rankLevel) {
        return rankLevelRepo.save(rankLevel);
    }

    public RankSetting saveRankSetting(RankSetting setting) {
        return settingRepo.save(setting);
    }

    public StudentRank saveRank(StudentRank rank) {
        return rankRepo.save(rank);
    }

    public List<RankSetting> createRankSettingsForDojo(Dojo dojo, List<RankSettingDto> rankSettingDtos) {
        List<RankSetting> rankSettings = new ArrayList<>();
        if (!CollectionUtils.isEmpty(rankSettingDtos)) {

            RankName rankSettingRankName = null;
            for (RankSettingDto rankSettingDto : rankSettingDtos) {
                if (rankSettingRankName == null || rankSettingDto.getRankName() != rankSettingRankName.getName()) {
                    Optional<RankName> rankName = rankNameRepo.findByName(rankSettingDto.getRankName());

                    if (rankName.isPresent()) {
                        rankSettingRankName = rankName.get();
                    } else {
                        rankSettingRankName = this.saveRankName(new RankName(rankSettingDto.getRankName()));
                    }
                }
                RankLevel rankLevel = new RankLevel(rankSettingDto.getSortOrder(), rankSettingDto.getNumber(),
                        rankSettingDto.getHoursRequired(),
                        rankSettingDto.getBeltColor(), rankSettingDto.getStripeColor(),
                        rankSettingDto.getStripeCount());
                rankLevel = saveRankLevel(rankLevel);

                RankSetting newSetting = saveRankSetting(new RankSetting(dojo, rankSettingRankName, rankLevel));
                rankSettings.add(newSetting);
            }
        }
        return rankSettings;
    }

    public List<RankSetting> getRankSettingsByDojo(Dojo dojo) {
        Optional<List<RankSetting>> optional = settingRepo.findByDojo(dojo);

        return optional.isPresent() ? optional.get() : Collections.emptyList();
    }
}