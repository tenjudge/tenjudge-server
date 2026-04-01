package io.github.yush1x.tenjudge.server.auth.persistence;

import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequestDTO;
import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final UserMapper userMapper;

    @Transactional
    public Long insert(RegisterRequestDTO registerRequestDTO) {
        User user = new User();
        user.setUsername(registerRequestDTO.getUsername());
        user.setPassword(registerRequestDTO.getPassword());
        user.setEmail(registerRequestDTO.getEmail());
        user.setRole(registerRequestDTO.getRole());
        userMapper.insert(user);
        return user.getId();
    }

}
