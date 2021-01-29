package com.dojomanager.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.rank.BeltColor;
import com.dojomanager.data.entities.rank.RankLevel;
import com.dojomanager.data.entities.rank.RankName;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.data.repositories.rank.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RankService {

    @Autowired
    RankNameRepository rankNameRepo;
    @Autowired
    RankLevelRepository rankLevelRepo;
    @Autowired
    BeltColorRepository beltRepo;
    @Autowired
    RankSettingRepository settingRepo;

    public RankName saveRankName(RankName rank) {
        return rankNameRepo.save(rank);
    }

    public RankLevel saveRankLevel(RankLevel rankLevel) {
        return rankLevelRepo.save(rankLevel);
    }

    public BeltColor saveBeltColor(BeltColor belt) {
        return beltRepo.save(belt);
    }

    public RankSetting saveRankSetting(RankSetting setting) {
        return settingRepo.save(setting);
    }

    public List<RankSetting> getRankSettingsByDojo(Dojo dojo) {
        Optional<List<RankSetting>> optional = settingRepo.findByDojo(dojo);

        return optional.isPresent() ? optional.get() : Collections.emptyList();
    }
} 