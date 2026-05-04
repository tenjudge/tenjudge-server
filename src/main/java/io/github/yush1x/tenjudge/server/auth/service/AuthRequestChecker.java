package io.github.yush1x.tenjudge.server.auth.service;

import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequest;
import io.github.yush1x.tenjudge.server.auth.dto.UserRoleUpdateRequest;
import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.auth.utils.Validator;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


/*
 * 请求检查器，检查请求是否合法
 */

@Service
@RequiredArgsConstructor
public class AuthRequestChecker {

    private final UserQueryService userQueryService;

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

        // 注册前先检查唯一字段，尽早给出明确业务错误，避免无意义的插库尝试。
        if (userQueryService.selectByUsername(registerRequest.getUsername()) != null) {
            throw new BizException(Code.USERNAME_ALREADY_EXISTS);
        }
        if (userQueryService.selectByEmail(registerRequest.getEmail()) != null) {
            throw new BizException(Code.EMAIL_ALREADY_EXISTS);
        }
    }

    public void checkPublicUserQuery(Long userId, String username) {
        // 公开用户查询只允许使用一个稳定身份字段，避免接口语义变成模糊搜索。
        if ((userId == null && username == null) || (userId != null && username != null)) {
            throw new BizException(Code.USER_REQUEST_INVALID, "exactly one query condition is required");
        }
        if (userId != null && userId <= 0) {
            throw new BizException(Code.USER_REQUEST_INVALID, "userId is invalid");
        }
        if (username != null && !Validator.isUsernameValid(username)) {
            throw new BizException(Code.USER_REQUEST_INVALID, "username is invalid");
        }
    }

    public void checkUserRoleUpdateRequest(UserRoleUpdateRequest request) {
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            throw new BizException(Code.USER_REQUEST_INVALID, "userId is invalid");
        }
        if (request.getRole() == null || !Validator.isRoleValid(request.getRole())) {
            throw new BizException(Code.ROLE_INVALID);
        }
    }
}
