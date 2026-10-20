package com.dojomanager.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.rank.BeltColor;
import com.dojomanager.data.entities.rank.StudentRank;
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
    @Autowired
    RankRepository rankRepo;

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

    public StudentRank saveRank(StudentRank rank) {
        return rankRepo.save(rank);
    }

    public Optional<RankName> findRankNameById(Long id) {
        return rankNameRepo.findById(id);
    }

    public List<RankSetting> findSettingsByRankName(RankName rankName) {
        return settingRepo.findByRankName(rankName);
    }

    public boolean isRankSettingAssigned(RankSetting setting) {
        return rankRepo.existsByRankSetting(setting);
    }

    public void deleteRankSetting(RankSetting setting) {
        settingRepo.delete(setting);
    }

    public void deleteBeltColor(BeltColor belt) {
        beltRepo.delete(belt);
    }

    public void deleteRankLevel(RankLevel rankLevel) {
        rankLevelRepo.delete(rankLevel);
    }

    public void deleteRankName(RankName rankName) {
        rankNameRepo.delete(rankName);
    }

    public List<RankSetting> getRankSettingsByDojo(Dojo dojo) {
        Optional<List<RankSetting>> optional = settingRepo.findByDojo(dojo);

        return optional.isPresent() ? optional.get() : Collections.emptyList();
    }
} 