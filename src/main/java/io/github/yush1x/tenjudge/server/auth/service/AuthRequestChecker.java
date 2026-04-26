package io.github.yush1x.tenjudge.server.auth.service;

import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequest;
import io.github.yush1x.tenjudge.server.auth.utils.Validator;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import org.springframework.stereotype.Service;


/*
 * 请求检查器，检查请求是否合法
 */

@Service
public class AuthRequestChecker {

    public void checkRegisterRequest(RegisterRequest registerRequest) {
        if (registerRequest.getUsername() == null || !Validator.isUsernameValid(registerRequest.getUsername())) {
            throw new BizException(Code.USERNAME_INVALID);
        }
        if (registerRequest.getPassword() == null || !Validator.isPasswordValid(registerRequest.getPassword())) {
            throw new BizException(Code.PASSWORD_INVALID);
        }
        if (registerRequest.getEmail() == null || !Validator.isEmailValid(registerRequest.getEmail())) {
            throw new BizException(Code.EMAIL_INVALID);
        }
        if (registerRequest.getRole() == null || !Validator.isRoleValid(registerRequest.getRole())) {
            throw new BizException(Code.ROLE_INVALID);
        }
    }
}
