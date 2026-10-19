package com.dojomanager.services;

import com.dojomanager.data.dto.schedule.ScheduleSessionDTO;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.schedule.DailySchedule;
import com.dojomanager.data.entities.schedule.WeeklySchedule;
import com.dojomanager.data.repositories.schedule.DailyScheduleRepository;
import com.dojomanager.data.repositories.schedule.WeeklyScheduleRepository;
import com.dojomanager.data.repositories.dojo.DojoOwnerRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ScheduleService {
    @Autowired
    private WeeklyScheduleRepository weeklyRepo;
    @Autowired
    private DailyScheduleRepository dailyRepo;
    @Autowired
    private DojoOwnerRepository ownerRepo;

    public WeeklySchedule saveWeeklySchedule(WeeklySchedule weekSched) {
        return weeklyRepo.save(weekSched);
    }

    public DailySchedule saveDailySchedule(DailySchedule daySched) {
        return dailyRepo.save(daySched);
    }

    public ScheduleSessionDTO saveSession(ScheduleSessionDTO session) {
        WeeklySchedule entity = new WeeklySchedule();
        entity.setDayOfWeek(DayOfWeek.valueOf(session.getDayOfWeek()));
        entity.setStartTime(LocalTime.parse(session.getStartTime()));
        entity.setEndTime(LocalTime.parse(session.getEndTime()));
        entity.setTitle(session.getTitle());
        return session;
    }

    public ScheduleSessionDTO updateSession(Long id, ScheduleSessionDTO session) {
        WeeklySchedule existing = weeklyRepo.findById(id)
            .orElse(null);
        if (existing == null) {
            return null;
        }
        existing.setDayOfWeek(DayOfWeek.valueOf(session.getDayOfWeek()));
        existing.setStartTime(LocalTime.parse(session.getStartTime()));
        existing.setEndTime(LocalTime.parse(session.getEndTime()));
        existing.setTitle(session.getTitle());
        weeklyRepo.save(existing);
        return session;
    }

    public ScheduleSessionDTO deleteSession(Long id) {
        weeklyRepo.deleteById(id);
        return null;
    }

    public ResponseEntity<Map<String, List<WeeklySchedule>>> getWeekView() {
        List<WeeklySchedule> sessions = weeklyRepo.findAll();
        Map<String, List<WeeklySchedule>> grouped = sessions.stream()
            .collect(Collectors.groupingBy(s -> s.getDayOfWeek().name()));
        return ResponseEntity.ok(grouped);
    }
}
