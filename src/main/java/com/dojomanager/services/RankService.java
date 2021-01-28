package com.dojomanager.services;

import com.dojomanager.data.entities.AbstractEntity;
import com.dojomanager.data.entities.rank.BeltColor;
import com.dojomanager.data.entities.rank.RankLevel;
import com.dojomanager.data.entities.rank.RankName;
import com.dojomanager.data.repositories.BeltColorRepository;
import com.dojomanager.data.repositories.RankLevelRepository;
import com.dojomanager.data.repositories.RankNameRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RankService{

    @Autowired
    RankNameRepository rankNameRepo;
    @Autowired
    RankLevelRepository rankLevelRepo;
    @Autowired
    BeltColorRepository beltRepo;

    public RankName saveRankName(RankName rank) {
        return rankNameRepo.save(rank);
    }

    public RankLevel saveRankLevel(RankLevel rankLevel) {
        return rankLevelRepo.save(rankLevel);
    }

    public BeltColor saveBeltColor(BeltColor belt) {
        return beltRepo.save(belt);
    }
} 