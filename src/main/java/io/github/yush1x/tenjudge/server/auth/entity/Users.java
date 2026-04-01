package io.github.yush1x.tenjudge.server.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class Users {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private String role;
    private Integer rating;
    private Integer maxRating;
    private String email;
    private String bio;
    private Integer solvedCount;
}
