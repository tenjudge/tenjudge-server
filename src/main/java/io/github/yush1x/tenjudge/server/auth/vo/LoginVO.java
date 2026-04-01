package io.github.yush1x.tenjudge.server.auth.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private UserVO userInfo;
}
