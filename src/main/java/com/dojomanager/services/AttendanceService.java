package com.dojomanager.services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.dojomanager.data.dto.attendance.AttendanceRecordDto;
import com.dojomanager.data.entities.attendance.AttendanceRecord;
import com.dojomanager.data.entities.attendance.AttendanceStatus;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.people.Student;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.data.entities.rank.StudentRank;
import com.dojomanager.data.entities.schedule.DailySchedule;
import com.dojomanager.data.entities.schedule.WeeklySchedule;
import com.dojomanager.data.forms.attendance.AttendanceCheckInForm;
import com.dojomanager.data.repositories.attendance.AttendanceRecordRepository;
import com.dojomanager.data.repositories.dojo.DojoOwnerRepository;
import com.dojomanager.data.repositories.dojo.DojoRepository;
import com.dojomanager.data.repositories.people.StudentRepository;
import com.dojomanager.data.repositories.rank.RankRepository;
import com.dojomanager.data.repositories.rank.RankSettingRepository;
import com.dojomanager.data.repositories.schedule.DailyScheduleRepository;
import com.dojomanager.data.repositories.schedule.WeeklyScheduleRepository;
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
    private final WeeklyScheduleRepository weeklyScheduleRepository;
    private final RankRepository rankRepository;
    private final RankSettingRepository rankSettingRepository;
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

        Long rawSessionId = form.getSessionId();
        LocalDate sessionDate = form.getDate();

        if (rawSessionId == null && sessionDate == null) {
            throw new BadRequestException("A sessionId or date is required");
        }
        if (rawSessionId != null && sessionDate == null) {
            throw new BadRequestException("A sessionId requires a date");
        }

        DailySchedule session = null;
        if (rawSessionId != null) {
            session = resolveDailySchedule(rawSessionId);
            verifySessionDojo(session, student);
        }

        Long resolvedSessionId = session != null ? session.getId() : null;
        Optional<AttendanceRecord> existing = attendanceRepository
                .findByStudentIdAndSessionIdAndSessionDate(student.getId(), resolvedSessionId, sessionDate);
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

        AttendanceRecord savedRecord = attendanceRepository.save(record);

        // Add class duration to student's time in rank
        if (form.getStatus() == AttendanceStatus.PRESENT || form.getStatus() == AttendanceStatus.LATE) {
            double durationHours = calculateDuration(session);
            StudentRank rank = student.getRank();
            if (rank != null) {
                rank.setHoursInRank(rank.getHoursInRank() + durationHours);
                rankRepository.save(rank);
            } else {
                List<RankSetting> settings = rankSettingRepository.findByDojo(dojo).orElse(List.of());
                if (!settings.isEmpty()) {
                    RankSetting lowest = settings.stream()
                            .filter(rs -> rs.getRankLevel() != null)
                            .min(Comparator.comparingInt(rs -> rs.getRankLevel().getSortOrder()))
                            .orElse(settings.get(0));
                    StudentRank newRank = new StudentRank(durationHours, lowest);
                    student.setRank(rankRepository.save(newRank));
                    studentRepository.save(student);
                }
            }
        }

        return savedRecord;
    }

    private double calculateDuration(DailySchedule session) {
        if (session == null) {
            return 1.0;
        }
        if (session.getHoursPerClass() > 0) {
            return session.getHoursPerClass();
        }
        if (session.getWeeklySchedule() != null && session.getWeeklySchedule().getStartTime() != null
                && session.getWeeklySchedule().getEndTime() != null) {
            long minutes = Duration.between(session.getWeeklySchedule().getStartTime(), session.getWeeklySchedule().getEndTime()).toMinutes();
            return Math.max(0.25, minutes / 60.0);
        }
        return 1.0;
    }

    private DailySchedule resolveDailySchedule(Long sessionId) {
        Optional<DailySchedule> dailyOpt = dailyScheduleRepository.findById(sessionId);
        if (dailyOpt.isPresent()) {
            return dailyOpt.get();
        }

        Optional<WeeklySchedule> weeklyOpt = weeklyScheduleRepository.findById(sessionId);
        if (weeklyOpt.isPresent()) {
            WeeklySchedule weekly = weeklyOpt.get();
            Optional<DailySchedule> existing = dailyScheduleRepository.findFirstByWeeklyScheduleId(weekly.getId());
            if (existing.isPresent()) {
                return existing.get();
            }
            double duration = 1.0;
            if (weekly.getStartTime() != null && weekly.getEndTime() != null) {
                long minutes = Duration.between(weekly.getStartTime(), weekly.getEndTime()).toMinutes();
                duration = Math.max(0.25, minutes / 60.0);
            }
            DailySchedule newDaily = new DailySchedule(duration, weekly.getStartTime(), weekly);
            return dailyScheduleRepository.save(newDaily);
        }

        throw new ResourceNotFoundException("Session " + sessionId + " was not found");
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> findBySession(OwnerPrincipal principal, Long sessionId, LocalDate date) {
        DojoOwner owner = requireOwner(principal);
        DailySchedule session = resolveDailySchedule(sessionId);
        verifySessionOwner(session, owner);
        return attendanceRepository.findBySessionIdAndSessionDate(session.getId(), date)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public void deleteAttendance(OwnerPrincipal principal, Long attendanceId) {
        DojoOwner owner = requireOwner(principal);
        AttendanceRecord record = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record " + attendanceId + " was not found"));

        if (!record.getDojo().getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenOperationException("Attendance record does not belong to your dojo");
        }

        rollbackRankCredit(record);

        attendanceRepository.delete(record);
    }

    /**
     * Cascade cleanup used when a schedule is deleted: reverts the hours credited
     * by the given sessions' attendance records and physically removes those
     * records before their {@code daily_schedule} parents are deleted.
     *
     * <p>Ownership is intentionally not checked here; {@code ScheduleService}
     * already verified that the schedule belongs to the caller. This method
     * deliberately does not depend on {@code ScheduleService} to avoid a
     * circular service dependency.</p>
     */
    @Transactional
    public void deleteRecordsForSessions(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        List<AttendanceRecord> records = attendanceRepository.findBySessionIdIn(sessionIds);
        for (AttendanceRecord record : records) {
            rollbackRankCredit(record);
        }

        attendanceRepository.deleteAll(records);
        attendanceRepository.flush();
    }

    /**
     * Reverts the rank hours credited by a PRESENT or LATE record. ABSENT
     * records never credited anything, so they are left alone.
     */
    private void rollbackRankCredit(AttendanceRecord record) {
        if (record.getStatus() == AttendanceStatus.PRESENT || record.getStatus() == AttendanceStatus.LATE) {
            double durationHours = calculateDuration(record.getSession());
            Student student = record.getStudent();
            if (student != null && student.getRank() != null) {
                StudentRank rank = student.getRank();
                rank.setHoursInRank(Math.max(0.0, rank.getHoursInRank() - durationHours));
                rankRepository.save(rank);
            }
        }
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
