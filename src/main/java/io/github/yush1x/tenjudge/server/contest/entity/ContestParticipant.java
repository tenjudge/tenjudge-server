package io.github.yush1x.tenjudge.server.contest.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("contest_participant")
public class ContestParticipant {
    private Long contestId;
    private Long userId;
    private String username;
}
