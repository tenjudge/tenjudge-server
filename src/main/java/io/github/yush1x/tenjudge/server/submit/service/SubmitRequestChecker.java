package io.github.yush1x.tenjudge.server.submit.service;

import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.common.Language;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.submit.dto.JudgeRequest;
import org.springframework.stereotype.Service;

/*
检查提交请求参数是否合法
 */
@Service
public class SubmitRequestChecker {

    public void checkJudgeRequest(JudgeRequest judgeRequest) {
        if (judgeRequest == null) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "request is null");
        }
        if (judgeRequest.getProblemId() == null || judgeRequest.getProblemId() <= 0) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "problemId is required");
        }
        if (judgeRequest.getContestId() != null && judgeRequest.getContestId() <= 0) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "contestId must be > 0");
        }
        if (judgeRequest.getLanguage() == null || !Language.contains(judgeRequest.getLanguage())) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "language is invalid");
        }
        if (judgeRequest.getCode() == null || judgeRequest.getCode().trim().isEmpty()) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "code is required");
        }
        if (judgeRequest.getIsAgent() == null) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "isAgent is required");
        }
    }

    public void checkUserContestSubmissionListRequest(Long contestId, Long userId) {
        if (contestId == null || contestId <= 0) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "contestId is invalid");
        }
        if (userId == null || userId <= 0) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "userId is invalid");
        }
    }

    public void checkUserSubmissionPageRequest(Long userId, Long current, Long size) {
        if (userId == null || userId <= 0) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "userId is invalid");
        }
        if (current == null || current < 1) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "current is invalid");
        }
        if (size == null || size < 1 || size > 100) {
            throw new BizException(Code.SUBMIT_REQUEST_INVALID, "size is invalid");
        }
    }
}
