package io.github.yush1x.tenjudge.server.auth.utils;

import io.github.yush1x.tenjudge.server.auth.entity.User;
import io.github.yush1x.tenjudge.server.auth.vo.UserVO;

public class Converter {

    public static UserVO toUserVO(User user) {
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setEmail(user.getEmail());
        userVO.setBio(user.getBio());
        userVO.setRole(user.getRole());
        userVO.setCreatedAt(user.getCreatedAt());
        userVO.setRating(user.getRating());
        userVO.setMaxRating(user.getMaxRating());
        return userVO;
    }
}
