package com.dojomanager.data.forms;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class SignupForm {
    
    @NotBlank
    @Size(min = 3)
    private String firstName;
    
    @NotBlank
    @Size(min = 3)
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 128)
    private String password;
}
