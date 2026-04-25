package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemPermissionChecker {

    private final AuthService authService;

    public void check(String visibility, Long contestId, Boolean isAgent) {

        /*
         对于超级管理员和管理员，直接放行。
         对于普通用户，首先检查题目可见性：
           - 若题目为 public，则直接放行。
           - 若题目为 private，验证是否为比赛参赛者，且比赛正在进行中。
         */

        Long id = authService.checkLogin();
        String role = authService.getRole(id);
        if ("super_admin".equals(role) || "admin".equals(role)) return;
        if ("public".equals(visibility)) {
            return;
        } else if ("private".equals(visibility)) {
            if (contestId == null) {
                throw new BizException(Code.FORBIDDEN);
            }
            return;
            // TODO 验证是否为比赛参赛者，且比赛正在进行中
        } else {
            log.warn("题目可见性不合法，Unknown visibility: {}", visibility);
            throw new BizException(Code.FORBIDDEN);
        }

    }

}
