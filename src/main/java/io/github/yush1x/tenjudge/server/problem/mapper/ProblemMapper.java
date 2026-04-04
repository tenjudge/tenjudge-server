package io.github.yush1x.tenjudge.server.problem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProblemMapper extends BaseMapper<Problem> {
}
