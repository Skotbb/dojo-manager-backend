package com.dojomanager.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.dojomanager.data.dto.attendance.AttendanceRecordDto;
import com.dojomanager.data.entities.attendance.AttendanceRecord;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.people.Student;
import com.dojomanager.data.entities.schedule.DailySchedule;
import com.dojomanager.data.forms.attendance.AttendanceCheckInForm;
import com.dojomanager.data.repositories.attendance.AttendanceRecordRepository;
import com.dojomanager.data.repositories.dojo.DojoOwnerRepository;
import com.dojomanager.data.repositories.dojo.DojoRepository;
import com.dojomanager.data.repositories.people.StudentRepository;
import com.dojomanager.data.repositories.schedule.DailyScheduleRepository;
import com.dojomanager.services.exceptions.BadRequestException;
import com.dojomanager.services.exceptions.DuplicateResourceException;
import com.dojomanager.services.exceptions.ForbiddenOperationException;
import com.dojomanager.services.exceptions.ResourceNotFoundException;
import com.dojomanager.security.services.OwnerPrincipal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Attendance check-in logic.
 *
 * <p>A check-in is either session-based (a {@code sessionId} plus a date) or
 * date-based (a date on its own). In both cases the record is scoped to the
 * authenticated owner's students and dojo.</p>
 */
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final DojoRepository dojoRepository;
    private final DailyScheduleRepository dailyScheduleRepository;
    private final DojoOwnerRepository ownerRepository;

    /**
     * Create one or more check-ins. Accepts a single form or a JSON array of
     * forms so that both single and bulk check-ins share one code path.
     */
    @Transactional
    public List<AttendanceRecordDto> checkIn(OwnerPrincipal principal, List<AttendanceCheckInForm> forms) {
        DojoOwner owner = requireOwner(principal);
        return forms.stream()
                .map(form -> toDto(checkOne(owner, form)))
                .toList();
    }

    private AttendanceRecord checkOne(DojoOwner owner, AttendanceCheckInForm form) {
        Student student = requireOwnedStudent(owner, form.getStudentId());
        Dojo dojo = student.getDojo();

        Long sessionId = form.getSessionId();
        LocalDate sessionDate = form.getDate();

        if (sessionId == null && sessionDate == null) {
            throw new BadRequestException("A sessionId or date is required");
        }
        if (sessionId != null && sessionDate == null) {
            throw new BadRequestException("A sessionId requires a date");
        }

        DailySchedule session = null;
        if (sessionId != null) {
            session = dailyScheduleRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session " + sessionId + " was not found"));
            verifySessionDojo(session, student);
        }

        Optional<AttendanceRecord> existing = attendanceRepository
                .findByStudentIdAndSessionIdAndSessionDate(student.getId(), sessionId, sessionDate);
        if (existing.isPresent()) {
            throw new DuplicateResourceException(
                    "An attendance record already exists for this student on " + sessionDate);
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setStudent(student);
        record.setDojo(dojo);
        record.setSession(session);
        record.setSessionDate(sessionDate);
        record.setStatus(form.getStatus());
        record.setRecordedBy(owner.getId());
        LocalDateTime now = LocalDateTime.now();
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        return attendanceRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> findBySession(OwnerPrincipal principal, Long sessionId, LocalDate date) {
        DojoOwner owner = requireOwner(principal);
        DailySchedule session = dailyScheduleRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session " + sessionId + " was not found"));
        verifySessionOwner(session, owner);
        return attendanceRepository.findBySessionIdAndSessionDate(sessionId, date)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> findByDate(OwnerPrincipal principal, LocalDate date) {
        DojoOwner owner = requireOwner(principal);
        return attendanceRepository.findBySessionDateAndDojoOwnerId(date, owner.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> findByStudent(OwnerPrincipal principal, Long studentId) {
        DojoOwner owner = requireOwner(principal);
        requireOwnedStudent(owner, studentId);
        return attendanceRepository.findByStudentIdAndDojoOwnerId(studentId, owner.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttendanceRecordDto getById(OwnerPrincipal principal, Long id) {
        DojoOwner owner = requireOwner(principal);
        AttendanceRecord record = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record " + id + " was not found"));
        if (record.getDojo() == null || !owner.getId().equals(record.getDojo().getOwner().getId())) {
            throw new ResourceNotFoundException("Attendance record " + id + " was not found");
        }
        return toDto(record);
    }

    private void verifySessionOwner(DailySchedule session, DojoOwner owner) {
        Dojo sessionDojo = session.getWeeklySchedule() == null ? null : session.getWeeklySchedule().getDojo();
        if (sessionDojo == null || !owner.getId().equals(sessionDojo.getOwner().getId())) {
            throw new ForbiddenOperationException(
                    "Session " + session.getId() + " does not belong to the authenticated account");
        }
    }

    private void verifySessionDojo(DailySchedule session, Student student) {
        Dojo sessionDojo = session.getWeeklySchedule() == null ? null : session.getWeeklySchedule().getDojo();
        if (sessionDojo == null) {
            throw new ResourceNotFoundException(
                    "Session " + session.getId() + " is not attached to a dojo");
        }
        if (!sessionDojo.getId().equals(student.getDojo().getId())) {
            throw new ForbiddenOperationException(
                    "Session " + session.getId() + " does not belong to the student's dojo");
        }
    }

    private DojoOwner requireOwner(OwnerPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ForbiddenOperationException("An authenticated dojo owner is required");
        }
        return ownerRepository.findById(principal.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Authenticated account no longer exists"));
    }

    private Student requireOwnedStudent(DojoOwner owner, Long studentId) {
        return studentRepository.findByIdAndDojoOwnerId(studentId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student " + studentId + " was not found"));
    }

    private AttendanceRecordDto toDto(AttendanceRecord record) {
        return AttendanceRecordDto.from(record);
    }
}
