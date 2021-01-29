package com.dojomanager.data.repositories.rank;

import java.util.List;
import java.util.Optional;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.rank.RankSetting;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RankSettingRepository extends JpaRepository<RankSetting, Long>{
    Optional<List<RankSetting>> findByDojo(Dojo dojo);
}
