package com.dojomanager.data.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ResetPasswordForm {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 6, max = 128)
    private String password;
}
