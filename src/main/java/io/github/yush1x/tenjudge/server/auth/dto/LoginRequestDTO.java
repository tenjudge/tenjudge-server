package io.github.yush1x.tenjudge.server.auth.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    String account;
    String password;
}
