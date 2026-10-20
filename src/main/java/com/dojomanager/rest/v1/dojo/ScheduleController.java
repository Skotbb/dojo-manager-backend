package com.dojomanager.rest.v1.dojo;

import java.net.URI;
import java.util.List;
import java.util.Map;

import com.dojomanager.data.dto.schedule.ScheduleSessionDTO;
import com.dojomanager.security.services.OwnerPrincipal;
import com.dojomanager.services.ScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Schedule session management for the authenticated dojo owner.
 */
@RestController
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RequestMapping(path = "/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ScheduleSessionDTO> createSession(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @Valid @RequestBody ScheduleSessionDTO dto) {
        ScheduleSessionDTO saved = scheduleService.saveSession(principal, dto);
        return ResponseEntity.created(URI.create("/api/v1/schedules/" + saved.getId()))
                .body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleSessionDTO> updateSession(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ScheduleSessionDTO dto) {
        ScheduleSessionDTO updated = scheduleService.updateSession(principal, id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable Long id) {
        scheduleService.deleteSession(principal, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/week")
    public ResponseEntity<Map<String, List<ScheduleSessionDTO>>> getWeekView(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @RequestParam(name = "dojoId", required = false) Long dojoId) {
        return ResponseEntity.ok(scheduleService.getWeekView(principal, dojoId));
    }

    @PostMapping("/{id}/test-promote")
    public ResponseEntity<List<com.dojomanager.data.dto.student.StudentDto>> testPromote(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable Long id,
            @RequestBody List<Long> studentIds) {
        return ResponseEntity.ok(scheduleService.testPromote(principal, id, studentIds));
    }
}
