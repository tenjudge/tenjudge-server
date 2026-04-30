package io.github.yush1x.tenjudge.server.contest.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.mapper.ContestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContestQueryService {

    private final ContestMapper contestMapper;

    public Contest select(Long id) {
        LambdaQueryWrapper<Contest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contest::getId, id);
        return contestMapper.selectOne(wrapper);
    }

    public Page<Contest> selectPage(long current, long size) {
        /*
        索引优化：
        CREATE INDEX idx_contest_start_time_id ON contest (start_time DESC, id DESC);
         */
        Page<Contest> page = new Page<>(current, size);
        LambdaQueryWrapper<Contest> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Contest::getStartTime)
                .orderByDesc(Contest::getId);
        return contestMapper.selectPage(page, wrapper);
    }
}
