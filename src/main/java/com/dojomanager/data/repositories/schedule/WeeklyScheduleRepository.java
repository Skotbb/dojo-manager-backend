package com.dojomanager.data.repositories.schedule;

import com.dojomanager.data.entities.schedule.WeeklySchedule;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Long>{
    
}
