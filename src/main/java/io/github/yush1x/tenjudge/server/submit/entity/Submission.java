package io.github.yush1x.tenjudge.server.submit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("submission")
public class Submission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;
    private Long problemId;
    private Long submitterId;
    private LocalDateTime submitTime;
    private Long contestId;
    private String language;
    private String status;
    private Integer timeUsedMs;
    private Integer memoryUsedMb;
    private String info;
}
