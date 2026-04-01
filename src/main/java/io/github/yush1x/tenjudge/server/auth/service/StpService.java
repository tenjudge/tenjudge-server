package io.github.yush1x.tenjudge.server.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Service;

/*
 * 把 StpUtil 的调用封装到一个 Spring 管理的 Bean, 方便单元测试
 * 其他所有业务逻辑均须使用StpService，禁止使用StpUtil
 */

@Service
public class StpService {

    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    public Long getLoginIdAsLong() {
        return StpUtil.getLoginIdAsLong();
    }


}
