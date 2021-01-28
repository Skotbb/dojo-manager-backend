package com.dojomanager.data.repositories;

import com.dojomanager.data.entities.rank.RankName;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RankNameRepository extends JpaRepository<RankName, Long> {
    
}
