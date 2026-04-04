package io.github.yush1x.tenjudge.server.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("problem_tag")
public class ProblemTag {
    private Long problemId;
    private String tag;
}
