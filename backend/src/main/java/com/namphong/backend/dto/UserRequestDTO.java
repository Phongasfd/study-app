package com.namphong.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDTO {
    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @Size(min=6)
    private String password;
}
// for registering