package io.github.yush1x.tenjudge.server.auth.persistence;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequest;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final UserMapper userMapper;

    // 插入user，返回id
    @Transactional(rollbackFor = Exception.class)
    public Long insert(RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setEmail(registerRequest.getEmail());
        user.setRole(registerRequest.getRole());
        userMapper.insert(user);
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(Long userId, String role) {
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId)
                .set(User::getRole, role);
        return userMapper.update(null, updateWrapper) > 0;
    }

}
