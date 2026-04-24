package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProblemPermissionChecker {

    private final AuthService authService;

    public void check(Problem problem) {

        /*
         * - 对于超级管理员和管理员，直接放行。
         * - 对于普通用户，首先检查题目可见性：
         *   - 若题目为 public，则直接放行。
         *   - 若题目为 private，则拒绝访问。
         *   - 若题目为 contest，需验证当前用户是否为该 contest 的参赛者，若验证通过则放行，否则拒绝访问。
         */

        Long id = authService.checkLogin();
        String role = authService.getRole(id);
        if ("super_admin".equals(role) || "admin".equals(role)) return;
        if ("public".equals(problem.getVisibility())) return;
        if ("private".equals(problem.getVisibility())) throw new BizException(Code.FORBIDDEN);

        // TODO 验证contest情况


    }

}
