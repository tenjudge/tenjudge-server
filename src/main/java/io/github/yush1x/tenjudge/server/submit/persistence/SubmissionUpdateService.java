package io.github.yush1x.tenjudge.server.submit.persistence;

import io.github.yush1x.tenjudge.server.submit.entity.Submission;
import io.github.yush1x.tenjudge.server.submit.mapper.SubmissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionUpdateService {

    private final SubmissionMapper submissionMapper;

    public void insert(Submission submission) {
        submissionMapper.insert(submission);
    }
}
