package io.github.yush1x.tenjudge.server.contest.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("contest_problem")
public class ContestProblem {
    private Long contestId;
    private Long problemId;
    private String problemIndex; // 题目在比赛中的索引 类似 A B C ...
}