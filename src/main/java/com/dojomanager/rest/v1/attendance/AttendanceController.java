package com.dojomanager.rest.v1.attendance;

import java.time.LocalDate;
import java.util.List;

import com.dojomanager.data.dto.attendance.AttendanceRecordDto;
import com.dojomanager.data.forms.attendance.AttendanceCheckInForm;
import com.dojomanager.services.AttendanceService;
import com.dojomanager.services.exceptions.BadRequestException;
import com.dojomanager.security.services.OwnerPrincipal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Owner-scoped attendance check-in endpoints.
 *
 * <p>Each endpoint is authenticated with the owner's JWT. The record's owner
 * scope is enforced in {@code AttendanceService}, so a request that carries a
 * valid token but targets another owner's student is refused there.</p>
 */
@RestController
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600)
@RequestMapping(path = "/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * Create a check-in. Accepts a single check-in object as the request body.
     */
    @PostMapping
    public ResponseEntity<List<AttendanceRecordDto>> checkIn(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @RequestBody AttendanceCheckInForm form) {
        if (form == null) {
            throw new BadRequestException("A request body is required");
        }
        List<AttendanceRecordDto> records = attendanceService.checkIn(principal, List.of(form));
        return ResponseEntity.status(HttpStatus.CREATED).body(records);
    }

    /**
     * Query check-ins by session or by date.
     */
    @GetMapping
    public ResponseEntity<List<AttendanceRecordDto>> list(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @RequestParam(name = "sessionId", required = false) Long sessionId,
            @RequestParam(name = "date", required = false) LocalDate date) {
        if (sessionId == null && date == null) {
            throw new BadRequestException("A sessionId or date is required");
        }
        if (sessionId != null && date == null) {
            throw new BadRequestException("A sessionId requires a date");
        }
        List<AttendanceRecordDto> records = sessionId != null
                ? attendanceService.findBySession(principal, sessionId, date)
                : attendanceService.findByDate(principal, date);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceRecordDto> getById(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(attendanceService.getById(principal, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(
            @AuthenticationPrincipal OwnerPrincipal principal,
            @PathVariable("id") Long id) {
        attendanceService.deleteAttendance(principal, id);
        return ResponseEntity.noContent().build();
    }
}
