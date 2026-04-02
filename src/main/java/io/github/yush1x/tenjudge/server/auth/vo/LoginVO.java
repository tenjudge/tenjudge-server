package io.github.yush1x.tenjudge.server.auth.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String tokenName;
    private String tokenValue;
    private UserVO userInfo;
}
