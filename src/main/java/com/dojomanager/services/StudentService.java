package com.dojomanager.services;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.dojo.DojoOwner;
import com.dojomanager.data.entities.people.Student;
import com.dojomanager.data.entities.rank.RankSetting;
import com.dojomanager.data.entities.rank.StudentRank;
import com.dojomanager.data.forms.student.StudentRequestForm;
import com.dojomanager.data.dto.student.StudentDto;
import com.dojomanager.data.repositories.dojo.DojoOwnerRepository;
import com.dojomanager.data.repositories.dojo.DojoRepository;
import com.dojomanager.data.repositories.people.StudentRepository;
import com.dojomanager.data.repositories.rank.RankRepository;
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
            student.setRank(requireRankInDojo(student.getDojo(), form.getRankId()));
        }

        return toDto(studentRepository.save(student));
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
        StudentRank rank = rankRepository.findById(rankId)
                .orElseThrow(() -> new ResourceNotFoundException("Rank " + rankId + " was not found"));

        RankSetting setting = rank.getRankSetting();
        Long rankDojoId = setting == null || setting.getDojo() == null ? null : setting.getDojo().getId();

        if (!dojo.getId().equals(rankDojoId)) {
            throw new ForbiddenOperationException(
                    "Rank " + rankId + " does not belong to dojo " + dojo.getId());
        }

        return rank;
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

        Optional.ofNullable(student.getRank()).ifPresent(rank -> {
            builder.rankId(rank.getId()).hoursInRank(rank.getHoursInRank());

            RankSetting setting = rank.getRankSetting();
            if (setting != null) {
                if (setting.getRankName() != null) {
                    builder.currentRankName(setting.getRankName().getName());
                }
                if (setting.getRankLevel() != null) {
                    builder.currentRankLevel(setting.getRankLevel().getNumber());
                }
            }
        });

        return builder.build();
    }
}
