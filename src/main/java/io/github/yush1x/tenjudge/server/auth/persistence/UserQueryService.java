package io.github.yush1x.tenjudge.server.auth.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserMapper userMapper;

    // 获取用户角色
    public String getRole(Long userId) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(User::getRole)
                .eq(User::getId, userId);
        User user = userMapper.selectOne(queryWrapper);
        return user.getRole();
    }

    // 获取用户加密后的密码（通过用户名）
    public User selectByUsername(String username) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(User::getPassword)
                .eq(User::getUsername, username);
        return userMapper.selectOne(queryWrapper);
    }

    // 获取用户加密后的密码（通过邮箱）
    public User selectByEmail(String email) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(User::getPassword)
                .eq(User::getEmail, email);
        return userMapper.selectOne(queryWrapper);
    }
}
