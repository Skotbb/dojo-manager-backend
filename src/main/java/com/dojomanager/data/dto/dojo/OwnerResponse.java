package com.dojomanager.data.dto.dojo;

import java.util.List;

import com.dojomanager.data.entities.dojo.DojoOwner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Flat representation of the authenticated dojo owner and the dojos it manages.
 *
 * <p>
 * Replaces the previous raw {@code DojoOwner} responses, which serialized the
 * JPA entity (including its lazy managed-dojos collection).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private List<DojoDto> managedDojos;

    public static OwnerResponse from(DojoOwner owner) {
        if (owner == null) {
            return null;
        }

        return OwnerResponse.builder()
                .id(owner.getId())
                .firstName(owner.getFirstName())
                .lastName(owner.getLastName())
                .email(owner.getEmail())
                .managedDojos(owner.getManagedDojos().stream().map(DojoDto::from).toList())
                .build();
    }
}
