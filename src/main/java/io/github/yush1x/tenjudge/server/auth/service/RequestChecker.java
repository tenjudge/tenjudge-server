package io.github.yush1x.tenjudge.server.auth.service;

import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequestDTO;
import io.github.yush1x.tenjudge.server.auth.utils.Validator;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import org.springframework.stereotype.Service;


/*
 * 请求检查器，检查请求是否合法
 */

@Service
public class RequestChecker {

    public void checkRegisterRequest(RegisterRequestDTO registerRequestDTO) {
        if (registerRequestDTO.getUsername() == null || !Validator.isUsernameValid(registerRequestDTO.getUsername())) {
            throw new BizException(Code.USERNAME_INVALID);
        }
        if (registerRequestDTO.getPassword() == null || !Validator.isPasswordValid(registerRequestDTO.getPassword())) {
            throw new BizException(Code.PASSWORD_INVALID);
        }
        if (registerRequestDTO.getEmail() == null || !Validator.isEmailValid(registerRequestDTO.getEmail())) {
            throw new BizException(Code.EMAIL_INVALID);
        }
        if (registerRequestDTO.getRole() == null || !Validator.isRoleValid(registerRequestDTO.getRole())) {
            throw new BizException(Code.ROLE_INVALID);
        }
    }
}
