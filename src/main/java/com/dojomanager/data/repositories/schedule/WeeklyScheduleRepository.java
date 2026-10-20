package com.dojomanager.data.repositories.schedule;

import java.util.Collection;
import java.util.List;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.schedule.WeeklySchedule;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Long> {
    List<WeeklySchedule> findByDojo(Dojo dojo);

    List<WeeklySchedule> findByDojoIn(Collection<Dojo> dojos);
}

