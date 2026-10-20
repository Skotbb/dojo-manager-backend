package com.dojomanager.data.dto.dojo;

import java.util.List;

import com.dojomanager.data.entities.dojo.Dojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat representation of a {@link Dojo}.
 *
 * <p>
 * The entity links back to its {@code DojoOwner}, its {@code Address} and its
 * students. Serializing it directly would leak that object graph, so the API
 * exposes this DTO instead.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DojoDto {

    private Long id;
    private String name;
    private String website;

    /** Flattened first line of the dojo's address, {@code null} when unset. */
    private String address;

    /** Rank ladder configured for this dojo. */
    private List<RankDto> ranks;

    public static DojoDto from(Dojo dojo) {
        return from(dojo, null);
    }

    public static DojoDto from(Dojo dojo, List<RankDto> ranks) {
        if (dojo == null) {
            return null;
        }

        return DojoDto.builder()
                .id(dojo.getId())
                .name(dojo.getName())
                .website(dojo.getWebsite())
                .address(dojo.getAddress() == null ? null : dojo.getAddress().getAddressLine1())
                .ranks(ranks)
                .build();
    }
}
