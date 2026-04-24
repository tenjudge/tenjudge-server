package io.github.yush1x.tenjudge.server.submit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.yush1x.tenjudge.server.submit.entity.Submission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SubmissionMapper extends BaseMapper<Submission> {
}
