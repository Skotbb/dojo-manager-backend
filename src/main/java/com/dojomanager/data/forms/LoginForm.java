package com.dojomanager.data.forms;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class LoginForm {
    @NotBlank
    @Size(min = 3, max = 60)
    private String email;

    @NotBlank
    @Size(min = 6, max = 128)
    private String password;
}
