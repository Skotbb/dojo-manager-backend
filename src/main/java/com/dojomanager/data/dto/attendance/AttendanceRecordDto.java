package com.dojomanager.data.dto.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.dojomanager.data.entities.attendance.AttendanceRecord;
import com.dojomanager.data.entities.attendance.AttendanceStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Public representation of an {@link AttendanceRecord}.
 */
@Getter
@Setter
@NoArgsConstructor
public class AttendanceRecordDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private Long dojoId;
    private Long sessionId;
    private LocalDate sessionDate;
    private AttendanceStatus status;
    private Long recordedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AttendanceRecordDto from(AttendanceRecord record) {
        AttendanceRecordDto dto = new AttendanceRecordDto();
        dto.setId(record.getId());
        dto.setStudentId(record.getStudent() == null ? null : record.getStudent().getId());
        dto.setStudentName(record.getStudent() == null ? null : studentName(record.getStudent()));
        dto.setDojoId(record.getDojo() == null ? null : record.getDojo().getId());
        dto.setSessionId(record.getSession() == null ? null : record.getSession().getId());
        dto.setSessionDate(record.getSessionDate());
        dto.setStatus(record.getStatus());
        dto.setRecordedBy(record.getRecordedBy());
        dto.setCreatedAt(record.getCreatedAt());
        dto.setUpdatedAt(record.getUpdatedAt());
        return dto;
    }

    private static String studentName(com.dojomanager.data.entities.people.Student student) {
        if (student == null) {
            return null;
        }
        String first = student.getFirstName() == null ? "" : student.getFirstName().trim();
        String last = student.getLastName() == null ? "" : student.getLastName().trim();
        if (first.isEmpty() && last.isEmpty()) {
            return null;
        }
        String name = (first + " " + last).trim();
        return name.isEmpty() ? null : name;
    }
}
