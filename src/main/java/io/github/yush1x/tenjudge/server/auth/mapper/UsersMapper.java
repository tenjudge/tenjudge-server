package io.github.yush1x.tenjudge.server.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.yush1x.tenjudge.server.auth.entity.Users;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UsersMapper extends BaseMapper<Users> {
}
