package io.github.yush1x.tenjudge.server.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContestProblemMapper extends BaseMapper<ContestProblem> {
}
