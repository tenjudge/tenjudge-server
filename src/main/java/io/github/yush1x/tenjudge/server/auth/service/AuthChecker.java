package io.github.yush1x.tenjudge.server.auth.service;

import io.github.yush1x.tenjudge.server.auth.persistence.UserQueryService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.infra.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/*
 * 检查用户权限是否满足要求
 */

@Service
@RequiredArgsConstructor
public class AuthChecker {

    private final StpService stpService;
    private final UserQueryService userQueryService;
    private final RedisService redisService;

    /*
     * 根据id获取用户角色
     */
    public String getRole(Long id) {
        String role = redisService.getValue("user:role:" + id, String.class);
        if (role == null) {
            role = userQueryService.getRole(id);
            if (role != null) {
                redisService.set("user:role:" + id, role, "user-role");
            }
        }
        return role;
    }

    /*
     * 检查用户是否登录，未登录则抛出异常，已登录则返回用户ID
     */
    public Long checkLogin() {
        if (!stpService.isLogin()) {
            throw new BizException(Code.UNAUTHORIZED);
        }
        return stpService.getLoginIdAsLong();
    }


    /*
     * 检查是否是管理员，不是则抛出异常，是则返回用户ID
     */
    public Long checkAdmin() {
        if (!stpService.isLogin()) {
            throw new BizException(Code.UNAUTHORIZED);
        }
        Long userId = stpService.getLoginIdAsLong();
        String role = getRole(userId);
        if (!"admin".equals(role) && !"super_admin".equals(role)) {
            throw new BizException(Code.FORBIDDEN);
        }
        return userId;
    }

    /*
     * 检查是否是超级管理员，不是则抛出异常，是则返回用户ID
     */
    public Long checkSuperAdmin() {
        if (!stpService.isLogin()) {
            throw new BizException(Code.UNAUTHORIZED);
        }
        Long userId = stpService.getLoginIdAsLong();
        String role = getRole(userId);
        if (!"super_admin".equals(role)) {
            throw new BizException(Code.FORBIDDEN);
        }
        return userId;
    }

}
