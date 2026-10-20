package com.dojomanager.rest.v1.students;

import java.util.List;
import java.util.Map;

import com.dojomanager.data.dto.attendance.AttendanceRecordDto;
import com.dojomanager.data.dto.student.StudentDto;
import com.dojomanager.data.forms.student.StudentRequestForm;
import com.dojomanager.security.services.OwnerPrincipal;
import com.dojomanager.services.AttendanceService;
import com.dojomanager.services.StudentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Student management for the authenticated dojo owner.
 *
 * <p>
 * The owner is taken from the security context, never from the payload, and
 * every operation is scoped to that owner's dojos by {@link StudentService}.
 * Responses are DTOs, so no JPA entity (and therefore no lazy object graph) is
 * ever serialized.
 * </p>
 */
@RestController
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RequestMapping(path = "/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    private final AttendanceService attendanceService;

    @PostMapping
    public ResponseEntity<StudentDto> createStudent(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @Valid @RequestBody StudentRequestForm form) {

        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(principal, form));
    }

    @GetMapping
    public List<StudentDto> listStudents(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @RequestParam(name = "dojoId", required = false) Long dojoId,
            @RequestParam(name = "active", required = false) Boolean active) {

        return studentService.listStudents(principal, dojoId, active);
    }

    @GetMapping("/eligible-for-promotion")
    public List<StudentDto> getEligibleForPromotion(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @RequestParam(name = "dojoId", required = false) Long dojoId) {
        return studentService.getEligibleForPromotion(principal, dojoId);
    }

    @GetMapping("/{id}")
    public StudentDto getStudent(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id) {

        return studentService.getStudent(principal, id);
    }

    @PutMapping("/{id}")
    public StudentDto updateStudent(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @Valid @RequestBody StudentRequestForm form) {

        return studentService.updateStudent(principal, id, form);
    }

    @PutMapping("/{id}/schedules")
    public StudentDto setEnrolledSchedules(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @RequestBody List<Long> scheduleIds) {
        return studentService.setEnrolledSchedules(principal, id, scheduleIds);
    }

    /**
     * Soft delete: the student is deactivated, its rank and rank history are kept.
     */
    @DeleteMapping("/{id}")
    public StudentDto deactivateStudent(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id) {

        return studentService.deactivateStudent(principal, id);
    }

    @GetMapping("/{id}/attendance")
    public List<AttendanceRecordDto> getAttendanceHistory(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id) {
        return attendanceService.findByStudent(principal, id);
    }

    @PostMapping("/{id}/hours")
    public StudentDto addHours(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> body) {
        Number hours = (Number) body.getOrDefault("hours", 0);
        return studentService.addHours(principal, id, hours.doubleValue());
    }

    @PostMapping("/{id}/rank")
    public StudentDto assignRank(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> body) {
        Number rankSettingId = (Number) body.get("rankSettingId");
        if (rankSettingId == null) {
            throw new com.dojomanager.services.exceptions.BadRequestException("rankSettingId is required");
        }
        Number hours = (Number) body.get("hours");
        return studentService.assignRank(principal, id, rankSettingId.longValue(), hours != null ? hours.doubleValue() : 0.0);
    }

    @PostMapping("/{id}/promote")
    public StudentDto promoteStudent(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Long targetRankSettingId = null;
        if (body != null && body.get("rankSettingId") != null) {
            targetRankSettingId = ((Number) body.get("rankSettingId")).longValue();
        }
        return studentService.promoteStudent(principal, id, targetRankSettingId);
    }
}
