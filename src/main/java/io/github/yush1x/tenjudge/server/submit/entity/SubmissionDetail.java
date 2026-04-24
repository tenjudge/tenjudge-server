package io.github.yush1x.tenjudge.server.submit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("submission_detail")
public class SubmissionDetail {
    private Long submissionId;
    private Integer testCaseId;
    private String input;
    private String output;
    private String answer;
    private String info;
    private String status;
    private Integer timeUsedMs;
    private Integer memoryUsedMb;
}