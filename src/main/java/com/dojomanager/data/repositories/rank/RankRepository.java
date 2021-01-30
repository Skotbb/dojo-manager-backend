package com.dojomanager.data.repositories.rank;

import com.dojomanager.data.entities.rank.StudentRank;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RankRepository extends JpaRepository<StudentRank, Long>{
    
}
