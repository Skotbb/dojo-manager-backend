package com.dojomanager.services;

import com.dojomanager.data.entities.schedule.DailySchedule;
import com.dojomanager.data.entities.schedule.WeeklySchedule;
import com.dojomanager.data.repositories.schedule.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScheduleService {
    @Autowired
    private WeeklyScheduleRepository weeklyRepo;
    @Autowired
    private DailyScheduleRepository dailyRepo;

    public WeeklySchedule saveWeeklySchedule(WeeklySchedule weekSched) {
        return weeklyRepo.save(weekSched);
    }

    public DailySchedule saveDailySchedule(DailySchedule daySched) {
        return dailyRepo.save(daySched);
    }
}