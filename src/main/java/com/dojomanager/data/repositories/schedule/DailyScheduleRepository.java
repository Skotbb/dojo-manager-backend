package com.dojomanager.data.repositories.schedule;

import com.dojomanager.data.entities.schedule.DailySchedule;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyScheduleRepository extends JpaRepository<DailySchedule, Long> {
    
}
