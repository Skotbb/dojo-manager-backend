package com.dojomanager.data.repositories.schedule;

import java.util.List;
import java.util.Optional;

import com.dojomanager.data.entities.schedule.DailySchedule;
import com.dojomanager.data.entities.schedule.WeeklySchedule;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyScheduleRepository extends JpaRepository<DailySchedule, Long> {
    List<DailySchedule> findByWeeklySchedule(WeeklySchedule weeklySchedule);
    List<DailySchedule> findByWeeklyScheduleId(Long weeklyScheduleId);
    Optional<DailySchedule> findFirstByWeeklyScheduleId(Long weeklyScheduleId);
}
