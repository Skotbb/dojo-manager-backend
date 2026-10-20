package com.dojomanager.data.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class LoginForm {
    @NotBlank
    @Email
    @Size(min = 3, max = 60)
    private String email;

    @NotBlank
    @Size(min = 6, max = 128)
    private String password;
}
