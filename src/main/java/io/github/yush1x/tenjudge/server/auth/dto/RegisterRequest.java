package io.github.yush1x.tenjudge.server.auth.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String username;
    private String password;
    private String role;
    private String email;
}
