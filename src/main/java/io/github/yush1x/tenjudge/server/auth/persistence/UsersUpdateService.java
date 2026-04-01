package io.github.yush1x.tenjudge.server.auth.persistence;

import io.github.yush1x.tenjudge.server.auth.dto.RegisterRequestDTO;
import io.github.yush1x.tenjudge.server.auth.entity.Users;
import io.github.yush1x.tenjudge.server.auth.mapper.UsersMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsersUpdateService {

    private final UsersMapper usersMapper;

    @Transactional
    public Long insert(RegisterRequestDTO registerRequestDTO) {
        Users users = new Users();
        users.setUsername(registerRequestDTO.getUsername());
        users.setPassword(registerRequestDTO.getPassword());
        users.setEmail(registerRequestDTO.getEmail());
        users.setRole(registerRequestDTO.getRole());
        usersMapper.insert(users);
        return users.getId();
    }

}
