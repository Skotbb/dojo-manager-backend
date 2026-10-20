package com.dojomanager.services;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.dojomanager.data.entities.AbstractEntity;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.people.Student;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.data.entities.rank.StudentRank;
import com.dojomanager.data.entities.schedule.WeeklySchedule;
import com.dojomanager.data.forms.student.StudentRequestForm;
import com.dojomanager.data.dto.student.StudentDto;
import com.dojomanager.data.repositories.dojo.DojoOwnerRepository;
import com.dojomanager.data.repositories.dojo.DojoRepository;
import com.dojomanager.data.repositories.people.StudentRepository;
import com.dojomanager.data.repositories.rank.RankRepository;
import com.dojomanager.data.repositories.rank.RankSettingRepository;
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
 * Owner-scoped student operations.
 *
 * <p>
 * Every method resolves the caller's {@code DojoOwner} from the authenticated
 * principal and then works only through owner/dojo scoped repository finders.
 * A student that is not reachable through the caller's own dojos is reported as
 * "not found" so the API never reveals that another owner's student exists.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final DojoRepository dojoRepository;
    private final DojoOwnerRepository dojoOwnerRepository;
    private final RankRepository rankRepository;
    private final RankSettingRepository rankSettingRepository;
    private final WeeklyScheduleRepository weeklyScheduleRepository;

    @Transactional(readOnly = true)
    public List<StudentDto> listStudents(OwnerPrincipal principal, Long dojoId, Boolean active) {
        DojoOwner owner = requireOwner(principal);

        List<Student> students = dojoId == null
                ? studentRepository.findByDojoOwnerId(owner.getId())
                : studentRepository.findByDojoId(requireOwnedDojo(owner, dojoId).getId());

        return students.stream()
                .filter(student -> active == null || student.isActive() == active)
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentDto getStudent(OwnerPrincipal principal, Long studentId) {
        return toDto(requireOwnedStudent(principal, studentId));
    }

    @Transactional
    public StudentDto createStudent(OwnerPrincipal principal, StudentRequestForm form) {
        DojoOwner owner = requireOwner(principal);
        Dojo dojo = resolveTargetDojo(owner, form.getDojoId());

        String email = normaliseEmail(form.getEmail());
        if (studentRepository.existsByDojoIdAndEmailIgnoreCase(dojo.getId(), email)) {
            throw new DuplicateResourceException(
                    "A student with the email " + email + " already exists in this dojo");
        }

        Student student = new Student();
        student.setFirstName(trim(form.getFirstName()));
        student.setLastName(trim(form.getLastName()));
        student.setEmail(email);
        student.setActive(true);

        if (form.getRankId() != null) {
            student.setRank(requireRankInDojo(dojo, form.getRankId()));
        }

        if (form.getEnrolledScheduleIds() != null) {
            student.setEnrolledSchedules(resolveSchedulesInDojo(dojo, form.getEnrolledScheduleIds()));
        }

        // Keep both sides of the association consistent instead of relying on the FK only.
        dojo.addStudent(student);

        return toDto(studentRepository.save(student));
    }

    @Transactional
    public StudentDto updateStudent(OwnerPrincipal principal, Long studentId, StudentRequestForm form) {
        Student student = requireOwnedStudent(principal, studentId);

        String email = normaliseEmail(form.getEmail());
        if (!email.equalsIgnoreCase(normaliseEmail(student.getEmail()))
                && studentRepository.existsByDojoIdAndEmailIgnoreCase(student.getDojo().getId(), email)) {
            throw new DuplicateResourceException(
                    "A student with the email " + email + " already exists in this dojo");
        }

        student.setFirstName(trim(form.getFirstName()));
        student.setLastName(trim(form.getLastName()));
        student.setEmail(email);

        // A null rankId means "do not touch the rank": never silently clear rank history.
        if (form.getRankId() != null) {
            applyRankToStudent(student, form.getRankId());
        }

        if (form.getEnrolledScheduleIds() != null) {
            student.setEnrolledSchedules(resolveSchedulesInDojo(student.getDojo(), form.getEnrolledScheduleIds()));
        }

        return toDto(studentRepository.save(student));
    }

    private void applyRankToStudent(Student student, Long rankId) {
        StudentRank currentRank = student.getRank();
        if (currentRank != null) {
            // If already pointing to the exact same StudentRank ID:
            if (rankId.equals(currentRank.getId())) {
                return; // Nothing changed, keep existing hours
            }

            // If pointing to the same RankSetting ID:
            if (currentRank.getRankSetting() != null && rankId.equals(currentRank.getRankSetting().getId())) {
                return; // Nothing changed, keep existing hours
            }

            // If it's a different RankSetting ID:
            Optional<RankSetting> settingOpt = rankSettingRepository.findById(rankId);
            if (settingOpt.isPresent()) {
                RankSetting newSetting = settingOpt.get();
                if (!student.getDojo().getId().equals(newSetting.getDojo().getId())) {
                    throw new ForbiddenOperationException("Rank setting does not belong to student's dojo");
                }
                // Update rank setting on the student's existing rank, preserving hours in rank!
                currentRank.setRankSetting(newSetting);
                rankRepository.save(currentRank);
                return;
            }

            // If it's a different StudentRank ID:
            Optional<StudentRank> rankOpt = rankRepository.findById(rankId);
            if (rankOpt.isPresent()) {
                StudentRank newRank = rankOpt.get();
                if (newRank.getRankSetting() != null && !student.getDojo().getId().equals(newRank.getRankSetting().getDojo().getId())) {
                    throw new ForbiddenOperationException("Rank does not belong to student's dojo");
                }
                student.setRank(newRank);
                return;
            }

            throw new ResourceNotFoundException("Rank " + rankId + " was not found");
        } else {
            // Student has no rank yet
            student.setRank(requireRankInDojo(student.getDojo(), rankId));
        }
    }

    @Transactional
    public StudentDto setEnrolledSchedules(OwnerPrincipal principal, Long studentId, List<Long> scheduleIds) {
        Student student = requireOwnedStudent(principal, studentId);
        student.setEnrolledSchedules(resolveSchedulesInDojo(student.getDojo(), scheduleIds));
        return toDto(studentRepository.save(student));
    }

    private Set<WeeklySchedule> resolveSchedulesInDojo(Dojo dojo, List<Long> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<WeeklySchedule> schedules = weeklyScheduleRepository.findAllById(scheduleIds);
        for (WeeklySchedule s : schedules) {
            if (s.getDojo() == null || !dojo.getId().equals(s.getDojo().getId())) {
                throw new ForbiddenOperationException("Schedule " + s.getId() + " does not belong to student's dojo");
            }
        }
        return new HashSet<>(schedules);
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getEligibleForPromotion(OwnerPrincipal principal, Long dojoId) {
        return listStudents(principal, dojoId, true).stream()
                .filter(s -> Boolean.TRUE.equals(s.getEligibleForPromotion()))
                .toList();
    }

    /**
     * Deactivates a student. This is a soft delete: the record, its rank and its
     * rank history stay in place so a reactivated student keeps its history.
     */
    @Transactional
    public StudentDto deactivateStudent(OwnerPrincipal principal, Long studentId) {
        Student student = requireOwnedStudent(principal, studentId);
        student.setActive(false);
        return toDto(studentRepository.save(student));
    }

    /**
     * Adds training hours to the student's current rank.
     */
    @Transactional
    public StudentDto addHours(OwnerPrincipal principal, Long studentId, double hours) {
        if (hours <= 0) {
            throw new BadRequestException("Hours to add must be positive");
        }
        Student student = requireOwnedStudent(principal, studentId);
        StudentRank rank = student.getRank();
        if (rank == null) {
            throw new BadRequestException("Student has no rank assigned yet");
        }
        rank.setHoursInRank(rank.getHoursInRank() + hours);
        rankRepository.save(rank);
        return toDto(studentRepository.save(student));
    }

    /**
     * Assigns a specific rank setting to a student with initial hours.
     */
    @Transactional
    public StudentDto assignRank(OwnerPrincipal principal, Long studentId, Long rankSettingId, Double hours) {
        Student student = requireOwnedStudent(principal, studentId);
        RankSetting setting = rankSettingRepository.findById(rankSettingId)
                .orElseThrow(() -> new ResourceNotFoundException("RankSetting " + rankSettingId + " was not found"));

        if (!student.getDojo().getId().equals(setting.getDojo().getId())) {
            throw new ForbiddenOperationException("Rank setting does not belong to the student's dojo");
        }

        StudentRank rank = student.getRank();
        if (rank == null) {
            rank = new StudentRank(hours != null ? hours : 0.0, setting);
        } else {
            rank.setRankSetting(setting);
            if (hours != null) {
                rank.setHoursInRank(hours);
            }
        }
        student.setRank(rankRepository.save(rank));
        return toDto(studentRepository.save(student));
    }

    /**
     * Promotes a student to a target rank setting, or the next rank in the ladder.
     */
    @Transactional
    public StudentDto promoteStudent(OwnerPrincipal principal, Long studentId, Long targetRankSettingId) {
        Student student = requireOwnedStudent(principal, studentId);
        RankSetting nextSetting;

        if (targetRankSettingId != null) {
            nextSetting = rankSettingRepository.findById(targetRankSettingId)
                    .orElseThrow(() -> new ResourceNotFoundException("RankSetting " + targetRankSettingId + " was not found"));
            if (!student.getDojo().getId().equals(nextSetting.getDojo().getId())) {
                throw new ForbiddenOperationException("Target rank does not belong to the student's dojo");
            }
        } else {
            List<RankSetting> settings = rankSettingRepository.findByDojo(student.getDojo()).orElse(List.of());
            StudentRank currentRank = student.getRank();
            int currentSortOrder = (currentRank != null && currentRank.getRankSetting() != null && currentRank.getRankSetting().getRankLevel() != null)
                    ? currentRank.getRankSetting().getRankLevel().getSortOrder()
                    : -1;

            nextSetting = settings.stream()
                    .filter(rs -> rs.getRankLevel() != null && rs.getRankLevel().getSortOrder() > currentSortOrder)
                    .min(Comparator.comparingInt(rs -> rs.getRankLevel().getSortOrder()))
                    .orElseThrow(() -> new BadRequestException("Student is already at the highest configured rank"));
        }

        StudentRank rank = student.getRank();
        if (rank == null) {
            rank = new StudentRank(0.0, nextSetting);
        } else {
            rank.setRankSetting(nextSetting);
            rank.setHoursInRank(0.0);
        }
        student.setRank(rankRepository.save(rank));
        return toDto(studentRepository.save(student));
    }

    private DojoOwner requireOwner(OwnerPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new ForbiddenOperationException("An authenticated dojo owner is required");
        }

        return dojoOwnerRepository.findById(principal.getId())
                .orElseThrow(() -> new ForbiddenOperationException("Authenticated account no longer exists"));
    }

    private Student requireOwnedStudent(OwnerPrincipal principal, Long studentId) {
        DojoOwner owner = requireOwner(principal);

        return studentRepository.findByIdAndDojoOwnerId(studentId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student " + studentId + " was not found"));
    }

    private Dojo requireOwnedDojo(DojoOwner owner, Long dojoId) {
        return dojoRepository.findByOwner(owner).stream()
                .filter(dojo -> dojoId.equals(dojo.getId()))
                .findFirst()
                .orElseThrow(() -> new ForbiddenOperationException(
                        "Dojo " + dojoId + " does not belong to the authenticated account"));
    }

    /**
     * The dojo always comes from the authenticated principal. A supplied
     * {@code dojoId} can only pick one of that principal's own dojos.
     */
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

    private StudentRank requireRankInDojo(Dojo dojo, Long rankId) {
        Optional<StudentRank> existingRank = rankRepository.findById(rankId);
        if (existingRank.isPresent()) {
            StudentRank rank = existingRank.get();
            RankSetting setting = rank.getRankSetting();
            Long rankDojoId = setting == null || setting.getDojo() == null ? null : setting.getDojo().getId();

            if (!dojo.getId().equals(rankDojoId)) {
                throw new ForbiddenOperationException(
                        "Rank " + rankId + " does not belong to dojo " + dojo.getId());
            }
            return rank;
        }

        RankSetting setting = rankSettingRepository.findById(rankId)
                .orElseThrow(() -> new ResourceNotFoundException("Rank " + rankId + " was not found"));

        Long rankDojoId = setting.getDojo() == null ? null : setting.getDojo().getId();
        if (!dojo.getId().equals(rankDojoId)) {
            throw new ForbiddenOperationException(
                    "Rank " + rankId + " does not belong to dojo " + dojo.getId());
        }

        StudentRank studentRank = new StudentRank(0.0, setting);
        return rankRepository.save(studentRank);
    }

    private String normaliseEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private StudentDto toDto(Student student) {
        StudentDto.StudentDtoBuilder builder = StudentDto.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .active(student.isActive())
                .dojoId(student.getDojo() == null ? null : student.getDojo().getId());

        if (student.getEnrolledSchedules() != null) {
            builder.enrolledScheduleIds(student.getEnrolledSchedules().stream()
                    .map(AbstractEntity::getId)
                    .toList());
        } else {
            builder.enrolledScheduleIds(List.of());
        }

        Optional.ofNullable(student.getRank()).ifPresent(rank -> {
            builder.rankId(rank.getId()).hoursInRank(rank.getHoursInRank());

            RankSetting setting = rank.getRankSetting();
            if (setting != null) {
                builder.rankSettingId(setting.getId());
                if (setting.getRankName() != null) {
                    builder.currentRankName(setting.getRankName().getName());
                }
                if (setting.getRankLevel() != null) {
                    builder.currentRankLevel(setting.getRankLevel().getNumber());
                    builder.hoursRequired(setting.getRankLevel().getHoursRequired());

                    int required = setting.getRankLevel().getHoursRequired();
                    boolean hoursMet = required > 0 && rank.getHoursInRank() >= required;

                    List<RankSetting> settings = rankSettingRepository.findByDojo(student.getDojo()).orElse(List.of());
                    int currentSortOrder = setting.getRankLevel().getSortOrder();
                    Optional<RankSetting> nextRank = settings.stream()
                            .filter(rs -> rs.getRankLevel() != null && rs.getRankLevel().getSortOrder() > currentSortOrder)
                            .min(Comparator.comparingInt(rs -> rs.getRankLevel().getSortOrder()));

                    if (nextRank.isPresent()) {
                        builder.nextRankSettingId(nextRank.get().getId());
                        if (nextRank.get().getRankName() != null) {
                            builder.nextRankName(nextRank.get().getRankName().getName());
                        }
                        builder.eligibleForPromotion(hoursMet);
                    } else {
                        builder.eligibleForPromotion(false);
                    }
                }
                if (setting.getBeltColor() != null) {
                    builder.currentBeltColor(setting.getBeltColor().getColor());
                    builder.currentStripeColor(setting.getBeltColor().getStripeColor());
                    builder.currentStripeCount(setting.getBeltColor().getStripeCount());
                }
            }
        });

        if (student.getRank() == null) {
            builder.eligibleForPromotion(false);
        }

        return builder.build();
    }
}
