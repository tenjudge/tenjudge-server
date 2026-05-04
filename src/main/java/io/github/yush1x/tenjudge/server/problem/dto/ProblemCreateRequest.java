package io.github.yush1x.tenjudge.server.problem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "创建题目的 multipart/form-data 请求")
public class ProblemCreateRequest {

    @Schema(description = "题目 zip 文件", type = "string", format = "binary")
    private MultipartFile zipFile;
}
