package io.github.yush1x.tenjudge.server.auth.dto;

import lombok.Data;

@Data
public class LoginRequest {
    String account;
    String password;
}
