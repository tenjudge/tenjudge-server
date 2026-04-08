package io.github.yush1x.tenjudge.server.problem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("problem")
public class Problem {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long authorId;
    private String visibility;
    private String status;
    private String judgeType;
    private Double timeLimit;
    private Double memoryLimit;
    private String name;
    private String statement;
    private String solution;
    private Integer difficulty;
    private String problem_key;
}
