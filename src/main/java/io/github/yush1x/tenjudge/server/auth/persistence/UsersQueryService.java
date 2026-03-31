package io.github.yush1x.tenjudge.server.auth.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.yush1x.tenjudge.server.auth.entity.Users;
import io.github.yush1x.tenjudge.server.auth.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsersQueryService {

    private final UsersMapper usersMapper;

    /*
     * 获取用户角色
     */
    public String getRole(Long userId) {
        LambdaQueryWrapper<Users> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Users::getRole)
                .eq(Users::getId, userId);
        Users users = usersMapper.selectOne(queryWrapper);
        return users.getRole();
    }

}
