package com.dojomanager.data.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat, owner-scoped representation of a {@code Student}.
 *
 * <p>
 * Deliberately a DTO instead of the JPA entity: the entity holds a lazy
 * {@code Dojo} (which in turn holds its {@code DojoOwner}) and a lazy
 * {@code StudentRank}, so serializing it would leak another aggregate's
 * object graph.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private boolean active;

    /** The dojo that owns this student. */
    private Long dojoId;

    /** Current rank instance, {@code null} when the student has no rank yet. */
    private Long rankId;
    private Long rankSettingId;
    private String currentRankName;
    private Integer currentRankLevel;
    private String currentBeltColor;
    private String currentStripeColor;
    private Integer currentStripeCount;
    private Double hoursInRank;
    private Integer hoursRequired;

    /** Reoccurring class schedules this student is enrolled in. */
    private java.util.List<Long> enrolledScheduleIds;

    /** Promotion readiness */
    private Boolean eligibleForPromotion;
    private String nextRankName;
    private Long nextRankSettingId;
}
