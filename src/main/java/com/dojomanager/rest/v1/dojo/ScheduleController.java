package com.dojomanager.rest.v1.dojo;

import com.dojomanager.data.dto.schedule.ScheduleSessionDTO;
import com.dojomanager.data.entities.schedule.WeeklySchedule;
import com.dojomanager.services.ScheduleService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ScheduleSessionDTO> createSession(@RequestBody ScheduleSessionDTO dto) {
        ScheduleSessionDTO saved = scheduleService.saveSession(dto);
        return ResponseEntity.created(null)
            .body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleSessionDTO> updateSession(
            @PathVariable Long id, @RequestBody ScheduleSessionDTO dto) {
        ScheduleSessionDTO updated = scheduleService.updateSession(id, dto);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        scheduleService.deleteSession(id);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/week")
    public ResponseEntity<Map<String, List<WeeklySchedule>>> getWeekView() {
        return scheduleService.getWeekView();
    }
}
