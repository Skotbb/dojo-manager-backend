package com.dojomanager.data.dto.dojo;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * Body for creating a dojo together with its initial rank configuration.
 *
 * <p>
 * Kept separate from {@link DojoUpdateRequest} so updates cannot silently
 * rewrite the rank graph.
 * </p>
 */
@Data
public class CreateDojoRequest {

    @NotBlank
    private String name;

    private String website;

    private String address;

    @Valid
    private List<RankRequest> ranks;

    @Data
    public static class RankRequest {

        @NotBlank
        private String name;

        @Valid
        private List<RankLevelRequest> levels;
    }

    @Data
    public static class RankLevelRequest {

        private int sortOrder;
        private int number;
        private int hoursRequired;

        @NotBlank
        private String beltColor;

        private String stripeColor;

        private Integer stripeCount;
    }
}
