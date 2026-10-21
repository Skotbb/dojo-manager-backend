package com.dojomanager.services;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dojomanager.data.dto.schedule.ScheduleSessionDTO;
import com.dojomanager.data.dto.student.StudentDto;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.schedule.DailySchedule;
import com.dojomanager.data.entities.schedule.WeeklySchedule;
import com.dojomanager.data.repositories.dojo.DojoOwnerRepository;
import com.dojomanager.data.repositories.dojo.DojoRepository;
import com.dojomanager.data.repositories.people.StudentRepository;
import com.dojomanager.data.repositories.schedule.DailyScheduleRepository;
import com.dojomanager.data.repositories.schedule.WeeklyScheduleRepository;
import com.dojomanager.security.services.OwnerPrincipal;
import com.dojomanager.services.exceptions.BadRequestException;
import com.dojomanager.services.exceptions.ForbiddenOperationException;
import com.dojomanager.services.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final WeeklyScheduleRepository weeklyRepo;
    private final DailyScheduleRepository dailyRepo;
    private final DojoOwnerRepository ownerRepo;
    private final DojoRepository dojoRepository;
    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final AttendanceService attendanceService;

    public WeeklySchedule saveWeeklySchedule(WeeklySchedule weekSched) {
        return weeklyRepo.save(weekSched);
    }

    public DailySchedule saveDailySchedule(DailySchedule daySched) {
        return dailyRepo.save(daySched);
    }

    @Transactional
    public ScheduleSessionDTO saveSession(OwnerPrincipal principal, ScheduleSessionDTO session) {
        DojoOwner owner = requireOwner(principal);
        Dojo dojo = resolveTargetDojo(owner, session.getDojoId());

        DayOfWeek day = parseDayOfWeek(session.getDayOfWeek());
        LocalTime start = parseTime(session.getStartTime(), "startTime");
        LocalTime end = parseTime(session.getEndTime(), "endTime");
        if (!start.isBefore(end)) {
            throw new BadRequestException("Start time must be before end time");
        }

        WeeklySchedule entity = new WeeklySchedule();
        entity.setDojo(dojo);
        entity.setDayOfWeek(day);
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setTitle(session.getTitle().trim());
        entity.setInstructorId(session.getInstructorId());
        entity.setIsTestEvent(session.getIsTestEvent());

        WeeklySchedule saved = weeklyRepo.save(entity);
        return ScheduleSessionDTO.from(saved);
    }

    @Transactional
    public ScheduleSessionDTO updateSession(OwnerPrincipal principal, Long id, ScheduleSessionDTO session) {
        DojoOwner owner = requireOwner(principal);
        WeeklySchedule existing = requireOwnedSchedule(owner, id);

        if (session.getDojoId() != null && (existing.getDojo() == null || !session.getDojoId().equals(existing.getDojo().getId()))) {
            Dojo newDojo = requireOwnedDojo(owner, session.getDojoId());
            existing.setDojo(newDojo);
        }

        DayOfWeek day = parseDayOfWeek(session.getDayOfWeek());
        LocalTime start = parseTime(session.getStartTime(), "startTime");
        LocalTime end = parseTime(session.getEndTime(), "endTime");
        if (!start.isBefore(end)) {
            throw new BadRequestException("Start time must be before end time");
        }

        existing.setDayOfWeek(day);
        existing.setStartTime(start);
        existing.setEndTime(end);
        existing.setTitle(session.getTitle().trim());
        existing.setInstructorId(session.getInstructorId());
        if (session.getIsTestEvent() != null) {
            existing.setIsTestEvent(session.getIsTestEvent());
        }

        WeeklySchedule saved = weeklyRepo.save(existing);
        return ScheduleSessionDTO.from(saved);
    }

    @Transactional
    public List<StudentDto> testPromote(OwnerPrincipal principal, Long scheduleId, List<Long> studentIds) {
        DojoOwner owner = requireOwner(principal);
        requireOwnedSchedule(owner, scheduleId);

        List<StudentDto> promoted = new ArrayList<>();
        if (studentIds != null) {
            for (Long sId : studentIds) {
                promoted.add(studentService.promoteStudent(principal, sId, null));
            }
        }
        return promoted;
    }

    /**
     * Deletes a schedule together with every row that references it. All foreign
     * keys are NO ACTION, so the dependants are removed explicitly and in
     * dependency order inside a single transaction:
     *
     * <ol>
     *   <li>attendance records (reverting the hours they credited),</li>
     *   <li>the enrollment join rows,</li>
     *   <li>the daily sessions,</li>
     *   <li>the weekly schedule itself.</li>
     * </ol>
     */
    @Transactional
    public void deleteSession(OwnerPrincipal principal, Long id) {
        DojoOwner owner = requireOwner(principal);
        WeeklySchedule existing = requireOwnedSchedule(owner, id);

        List<DailySchedule> sessions = dailyRepo.findByWeeklyScheduleId(id);
        List<Long> sessionIds = sessions.stream()
                .map(DailySchedule::getId)
                .toList();
        attendanceService.deleteRecordsForSessions(sessionIds);

        studentRepository.deleteEnrollmentsByScheduleId(id);

        dailyRepo.deleteAll(sessions);
        dailyRepo.flush();

        weeklyRepo.delete(existing);
    }

    @Transactional(readOnly = true)
    public Map<String, List<ScheduleSessionDTO>> getWeekView(OwnerPrincipal principal, Long dojoId) {
        DojoOwner owner = requireOwner(principal);
        List<WeeklySchedule> sessions;
        if (dojoId != null) {
            Dojo dojo = requireOwnedDojo(owner, dojoId);
            sessions = weeklyRepo.findByDojo(dojo);
        } else {
            List<Dojo> dojos = dojoRepository.findByOwner(owner);
            if (dojos.isEmpty()) {
                sessions = Collections.emptyList();
            } else {
                sessions = weeklyRepo.findByDojoIn(dojos);
            }
        }

        return sessions.stream()
                .map(ScheduleSessionDTO::from)
                .collect(Collectors.groupingBy(ScheduleSessionDTO::getDayOfWeek));
    }

    private DojoOwner requireOwner(OwnerPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ForbiddenOperationException("An authenticated owner account is required");
        }
        return ownerRepo.findById(principal.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Authenticated owner account does not exist"));
    }

    private Dojo requireOwnedDojo(DojoOwner owner, Long dojoId) {
        return dojoRepository.findByOwner(owner).stream()
                .filter(dojo -> dojoId.equals(dojo.getId()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenOperationException(
                        "Dojo " + dojoId + " does not belong to the authenticated account"));
    }

    private Dojo resolveTargetDojo(DojoOwner owner, Long requestedDojoId) {
        List<Dojo> owned = dojoRepository.findByOwner(owner);
        if (owned.isEmpty()) {
            throw new ForbiddenOperationException("The authenticated account does not own a dojo yet");
        }
        if (requestedDojoId != null) {
            return requireOwnedDojo(owner, requestedDojoId);
        }
        if (owned.size() == 1) {
            return owned.get(0);
        }
        throw new ForbiddenOperationException(
                "The authenticated account owns several dojos; select one with dojoId");
    }

    private WeeklySchedule requireOwnedSchedule(DojoOwner owner, Long scheduleId) {
        WeeklySchedule schedule = weeklyRepo.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule session " + scheduleId + " was not found"));

        if (schedule.getDojo() == null
                || schedule.getDojo().getOwner() == null
                || !schedule.getDojo().getOwner().getId().equals(owner.getId())) {
            throw new ForbiddenOperationException(
                    "Schedule " + scheduleId + " does not belong to the authenticated account");
        }

        return schedule;
    }

    private DayOfWeek parseDayOfWeek(String day) {
        if (day == null || day.trim().isEmpty()) {
            throw new BadRequestException("dayOfWeek is required");
        }
        try {
            return DayOfWeek.valueOf(day.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid dayOfWeek: " + day);
        }
    }

    private LocalTime parseTime(String time, String fieldName) {
        if (time == null || time.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " is required");
        }
        try {
            return LocalTime.parse(time.trim());
        } catch (Exception e) {
            throw new BadRequestException("Invalid format for " + fieldName + ": " + time + " (expected HH:mm)");
        }
    }
}
