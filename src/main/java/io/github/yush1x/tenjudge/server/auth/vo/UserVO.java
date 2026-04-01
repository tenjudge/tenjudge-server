package io.github.yush1x.tenjudge.server.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private LocalDateTime createdAt;
    private String role;
    private Integer rating;
    private Integer maxRating;
    private String email;
    private String bio;
    private Integer solvedCount;
}
