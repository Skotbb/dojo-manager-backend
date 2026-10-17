package com.dojomanager.data.forms.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * Create/update payload for a student.
 *
 * <p>
 * There is intentionally no free-form ownership field here: {@code dojoId} may
 * only <em>select</em> one of the dojos already owned by the authenticated
 * principal, and the service rejects any value that does not resolve to a dojo
 * owned by that principal. The dojo therefore always comes from the
 * authenticated principal, never from the client.
 * </p>
 */
@Data
public class StudentRequestForm {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 254, message = "Email must be at most 254 characters")
    private String email;

    /**
     * Optional current rank (a {@code StudentRank} belonging to one of the
     * principal's dojos). {@code null} on create means "no rank yet"; on update
     * {@code null} leaves the existing rank untouched.
     */
    private Long rankId;

    /**
     * Optional dojo selector for owners with more than one dojo. Values that do
     * not belong to the authenticated principal are rejected with 403.
     */
    private Long dojoId;
}
