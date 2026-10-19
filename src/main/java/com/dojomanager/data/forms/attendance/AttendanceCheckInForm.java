package com.dojomanager.data.forms.attendance;

import java.time.LocalDate;

import com.dojomanager.data.entities.attendance.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single check-in request.
 *
 * <p>Provide either a {@code sessionId} (which always requires a {@code date})
 * or a {@code date} on its own.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class AttendanceCheckInForm {

    @NotNull(message = "studentId is required")
    private Long studentId;

    @NotNull(message = "status is required")
    private AttendanceStatus status;

    private Long sessionId;

    private LocalDate date;
}
