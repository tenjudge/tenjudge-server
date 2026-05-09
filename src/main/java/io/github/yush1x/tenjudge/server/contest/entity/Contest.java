package io.github.yush1x.tenjudge.server.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("contest")
public class Contest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime freezeTime;
    private LocalDateTime boardRefreshedAt;
    private Integer penaltyPerWrong;
}
