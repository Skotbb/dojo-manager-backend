package com.dojomanager.data.entities.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.dojomanager.data.entities.AbstractEntity;
import com.dojomanager.data.entities.dojo.Dojo;
import com.dojomanager.data.entities.people.Student;
import com.dojomanager.data.entities.schedule.DailySchedule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single attendance check-in.
 *
 * <p>Every record is tied to a student, a dojo and a date. It may also be tied
 * to a specific {@link DailySchedule} session. A record is unique per student,
 * session and date so that the same student can only be checked in once for a
 * given session on a given day.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendance_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_attendance_record",
                columnNames = {"student_id", "session_id", "session_date"})})
public class AttendanceRecord extends AbstractEntity {

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dojo_id", nullable = false)
    private Dojo dojo;

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private DailySchedule session;

    @Getter @Setter
    @Column(name = "session_date")
    private LocalDate sessionDate;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Getter @Setter
    @Column(name = "recorded_by", nullable = false)
    private Long recordedBy;

    @Getter @Setter
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Getter @Setter
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
