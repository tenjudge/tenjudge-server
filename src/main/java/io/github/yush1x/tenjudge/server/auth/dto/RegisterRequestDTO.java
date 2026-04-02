package io.github.yush1x.tenjudge.server.auth.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {

    private String username;
    private String password;
    private String role;
    private String email;
}
