package io.github.yush1x.tenjudge.server.submit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeRequest {
    String type;
    Long problemId;
    Long submitterId;
    Long contestId;
    String language;
    String code;
}
