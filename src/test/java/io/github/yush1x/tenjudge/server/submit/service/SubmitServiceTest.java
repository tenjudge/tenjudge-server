package io.github.yush1x.tenjudge.server.submit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestProblem;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestProblemQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.exception.BizException;
import io.github.yush1x.tenjudge.server.infra.MinioService;
import io.github.yush1x.tenjudge.server.problem.entity.Problem;
import io.github.yush1x.tenjudge.server.problem.persistence.ProblemQueryService;
import io.github.yush1x.tenjudge.server.problem.service.ProblemPermissionChecker;
import io.github.yush1x.tenjudge.server.submit.entity.Submission;
import io.github.yush1x.tenjudge.server.submit.entity.SubmissionDetail;
import io.github.yush1x.tenjudge.server.submit.mq.Producer;
import io.github.yush1x.tenjudge.server.submit.persistence.SubmissionDetailQueryService;
import io.github.yush1x.tenjudge.server.submit.persistence.SubmissionQueryService;
import io.github.yush1x.tenjudge.server.submit.persistence.SubmissionUpdateService;
import io.github.yush1x.tenjudge.server.submit.vo.SubmissionListItemVO;
import io.github.yush1x.tenjudge.server.submit.vo.SubmissionPageVO;
import io.github.yush1x.tenjudge.server.submit.vo.SubmissionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubmitServiceTest {

    private AuthService authService;
    private SubmissionQueryService submissionQueryService;
    private SubmissionDetailQueryService submissionDetailQueryService;
    private MinioService minioService;
    private ProblemQueryService problemQueryService;
    private ContestProblemQueryService contestProblemQueryService;
    private ContestQueryService contestQueryService;
    private SubmitService submitService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        submissionQueryService = mock(SubmissionQueryService.class);
        submissionDetailQueryService = mock(SubmissionDetailQueryService.class);
        SubmissionUpdateService submissionUpdateService = mock(SubmissionUpdateService.class);
        minioService = mock(MinioService.class);
        Producer producer = mock(Producer.class);
        ProblemPermissionChecker problemPermissionChecker = mock(ProblemPermissionChecker.class);
        problemQueryService = mock(ProblemQueryService.class);
        contestProblemQueryService = mock(ContestProblemQueryService.class);
        contestQueryService = mock(ContestQueryService.class);

        submitService = new SubmitService(
                authService,
                submissionQueryService,
                submissionDetailQueryService,
                submissionUpdateService,
                minioService,
                producer,
                problemPermissionChecker,
                problemQueryService,
                contestProblemQueryService,
                new SubmitRequestChecker(),
                contestQueryService
        );
    }

    @Test
    void getSubmission_ownedSubmission_returnsDetail() throws Exception {
        LocalDateTime submitTime = LocalDateTime.now();
        Submission submission = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .submitterId(1L)
                .submitTime(submitTime)
                .language("cpp")
                .status("ACCEPTED")
                .timeUsedMs(128)
                .memoryUsedMb(64)
                .info("ok")
                .build();
        Problem problem = new Problem();
        problem.setId(1001L);
        problem.setName("A + B Problem");
        SubmissionDetail detail = SubmissionDetail.builder()
                .submissionId(3001L)
                .testCaseId(1)
                .status("ACCEPTED")
                .timeUsedMs(32)
                .memoryUsedMb(16)
                .info("ok")
                .input("1 2")
                .output("3")
                .answer("3")
                .build();

        when(authService.checkLogin()).thenReturn(1L);
        when(submissionQueryService.select(3001L)).thenReturn(submission);
        when(problemQueryService.select(1001L)).thenReturn(problem);
        when(submissionDetailQueryService.selectBySubmissionId(3001L)).thenReturn(List.of(detail));
        when(minioService.read("submission/3001/code")).thenReturn("int main(){}");

        SubmissionVO result = submitService.getSubmission(3001L);

        assertEquals(3001L, result.getId());
        assertEquals(1001L, result.getProblemId());
        assertEquals("A + B Problem", result.getProblemName());
        assertEquals(submitTime, result.getSubmitTime());
        assertEquals("cpp", result.getLanguage());
        assertEquals("ACCEPTED", result.getStatus());
        assertEquals(128, result.getTime());
        assertEquals(64, result.getMemory());
        assertEquals("ok", result.getInfo());
        assertEquals("int main(){}", result.getCode());
        assertEquals(1, result.getDetails().size());
        assertEquals(1, result.getDetails().get(0).getTestCaseId());
        assertEquals("3", result.getDetails().get(0).getAnswer());
    }

    @Test
    void getSubmission_submissionNotFound_throwsBizException() {
        when(authService.checkLogin()).thenReturn(1L);
        when(submissionQueryService.select(3001L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> submitService.getSubmission(3001L));

        assertEquals(Code.SUBMISSION_NOT_FOUND, ex.getCode());
        verifyNoInteractions(problemQueryService, submissionDetailQueryService, minioService, contestQueryService);
    }

    @Test
    void getSubmission_otherUserSubmission_throwsBizException() {
        Submission submission = Submission.builder()
                .id(3001L)
                .submitterId(2L)
                .build();

        when(authService.checkLogin()).thenReturn(1L);
        when(submissionQueryService.select(3001L)).thenReturn(submission);

        BizException ex = assertThrows(BizException.class, () -> submitService.getSubmission(3001L));

        assertEquals(Code.FORBIDDEN, ex.getCode());
        verifyNoInteractions(problemQueryService, submissionDetailQueryService, minioService, contestQueryService);
    }

    @Test
    void getSubmission_adminCanViewOtherUserSubmission() throws Exception {
        Submission submission = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .submitterId(2L)
                .language("java")
                .status("PENDING")
                .build();

        when(authService.checkLogin()).thenReturn(1L);
        when(authService.getRole(1L)).thenReturn("admin");
        when(submissionQueryService.select(3001L)).thenReturn(submission);
        when(problemQueryService.select(1001L)).thenReturn(null);
        when(submissionDetailQueryService.selectBySubmissionId(3001L)).thenReturn(List.of());
        when(minioService.read("submission/3001/code")).thenReturn("class Main {}");

        SubmissionVO result = submitService.getSubmission(3001L);

        assertEquals(3001L, result.getId());
        assertEquals("java", result.getLanguage());
        assertEquals("class Main {}", result.getCode());
    }

    @Test
    void getSubmission_runningContestOwnedSubmission_hidesDetails() throws Exception {
        Submission submission = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .submitterId(1L)
                .contestId(2001L)
                .language("cpp")
                .status("ACCEPTED")
                .build();
        Contest contest = Contest.builder()
                .id(2001L)
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        when(authService.checkLogin()).thenReturn(1L);
        when(submissionQueryService.select(3001L)).thenReturn(submission);
        when(problemQueryService.select(1001L)).thenReturn(null);
        when(contestQueryService.select(2001L)).thenReturn(contest);
        when(minioService.read("submission/3001/code")).thenReturn("int main(){}");

        SubmissionVO result = submitService.getSubmission(3001L);

        assertEquals(0, result.getDetails().size());
        verify(submissionDetailQueryService, never()).selectBySubmissionId(3001L);
    }

    @Test
    void getSubmission_endedContestOwnedSubmission_returnsDetails() throws Exception {
        Submission submission = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .submitterId(1L)
                .contestId(2001L)
                .language("cpp")
                .status("ACCEPTED")
                .build();
        Contest contest = Contest.builder()
                .id(2001L)
                .endTime(LocalDateTime.now().minusDays(1))
                .build();
        SubmissionDetail detail = SubmissionDetail.builder()
                .submissionId(3001L)
                .testCaseId(1)
                .status("ACCEPTED")
                .answer("3")
                .build();

        when(authService.checkLogin()).thenReturn(1L);
        when(submissionQueryService.select(3001L)).thenReturn(submission);
        when(problemQueryService.select(1001L)).thenReturn(null);
        when(contestQueryService.select(2001L)).thenReturn(contest);
        when(submissionDetailQueryService.selectBySubmissionId(3001L)).thenReturn(List.of(detail));
        when(minioService.read("submission/3001/code")).thenReturn("int main(){}");

        SubmissionVO result = submitService.getSubmission(3001L);

        assertEquals(1, result.getDetails().size());
        assertEquals("3", result.getDetails().get(0).getAnswer());
    }

    @Test
    void getSubmission_problemNotFound_returnsNullProblemName() throws Exception {
        Submission submission = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .submitterId(1L)
                .build();

        when(authService.checkLogin()).thenReturn(1L);
        when(submissionQueryService.select(3001L)).thenReturn(submission);
        when(problemQueryService.select(1001L)).thenReturn(null);
        when(submissionDetailQueryService.selectBySubmissionId(3001L)).thenReturn(List.of());
        when(minioService.read("submission/3001/code")).thenReturn("print(1)");

        SubmissionVO result = submitService.getSubmission(3001L);

        assertNull(result.getProblemName());
        assertEquals(0, result.getDetails().size());
    }

    @Test
    void queryUserContestSubmissions_returnsContestProblemDisplayName() {
        LocalDateTime submitTime = LocalDateTime.now();
        Submission submission = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .submitTime(submitTime)
                .language("cpp")
                .status("ACCEPTED")
                .timeUsedMs(128)
                .memoryUsedMb(64)
                .build();
        Problem problem = new Problem();
        problem.setId(1001L);
        problem.setName("A + B Problem");
        ContestProblem contestProblem = ContestProblem.builder()
                .contestId(2001L)
                .problemId(1001L)
                .problemIndex("A")
                .build();

        when(submissionQueryService.selectByContestIdAndSubmitterId(2001L, 1L)).thenReturn(List.of(submission));
        when(problemQueryService.selectNamesByIds(Set.of(1001L))).thenReturn(List.of(problem));
        when(contestProblemQueryService.selectByContestId(2001L)).thenReturn(List.of(contestProblem));

        List<SubmissionListItemVO> result = submitService.queryUserContestSubmissions(2001L, 1L);

        assertEquals(1, result.size());
        assertEquals(3001L, result.get(0).getSubmissionId());
        assertEquals("A. A + B Problem", result.get(0).getProblemName());
        assertEquals("cpp", result.get(0).getLanguage());
        assertEquals("ACCEPTED", result.get(0).getStatus());
        assertEquals(128, result.get(0).getTime());
        assertEquals(64, result.get(0).getMemory());
        assertEquals(submitTime, result.get(0).getSubmitTime());
    }

    @Test
    void queryUserContestSubmissions_problemNotFound_returnsNullProblemName() {
        Submission submission = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .language("cpp")
                .status("PENDING")
                .build();
        ContestProblem contestProblem = ContestProblem.builder()
                .contestId(2001L)
                .problemId(1001L)
                .problemIndex("A")
                .build();

        when(submissionQueryService.selectByContestIdAndSubmitterId(2001L, 1L)).thenReturn(List.of(submission));
        when(problemQueryService.selectNamesByIds(Set.of(1001L))).thenReturn(List.of());
        when(contestProblemQueryService.selectByContestId(2001L)).thenReturn(List.of(contestProblem));

        List<SubmissionListItemVO> result = submitService.queryUserContestSubmissions(2001L, 1L);

        assertNull(result.get(0).getProblemName());
    }

    @Test
    void queryUserContestSubmissions_guestDuringFreeze_hidesFrozenSubmissions() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 10, 0);
        LocalDateTime freezeTime = startTime.plusMinutes(30);
        Submission beforeFreeze = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .submitTime(freezeTime.minusMinutes(1))
                .language("cpp")
                .status("WRONG_ANSWER")
                .build();
        Submission atFreeze = Submission.builder()
                .id(3002L)
                .problemId(1001L)
                .submitTime(freezeTime)
                .language("cpp")
                .status("ACCEPTED")
                .build();
        Submission afterFreeze = Submission.builder()
                .id(3003L)
                .problemId(1001L)
                .submitTime(freezeTime.plusMinutes(1))
                .language("cpp")
                .status("ACCEPTED")
                .build();
        Contest contest = Contest.builder()
                .id(2001L)
                .freezeTime(freezeTime)
                .endTime(LocalDateTime.now().plusHours(1))
                .build();
        Problem problem = new Problem();
        problem.setId(1001L);
        problem.setName("A + B Problem");
        ContestProblem contestProblem = ContestProblem.builder()
                .contestId(2001L)
                .problemId(1001L)
                .problemIndex("A")
                .build();

        when(authService.isLogin()).thenReturn(false);
        when(submissionQueryService.selectByContestIdAndSubmitterId(2001L, 1L))
                .thenReturn(List.of(beforeFreeze, atFreeze, afterFreeze));
        when(contestQueryService.select(2001L)).thenReturn(contest);
        when(problemQueryService.selectNamesByIds(Set.of(1001L))).thenReturn(List.of(problem));
        when(contestProblemQueryService.selectByContestId(2001L)).thenReturn(List.of(contestProblem));

        List<SubmissionListItemVO> result = submitService.queryUserContestSubmissions(2001L, 1L);

        assertEquals(1, result.size());
        assertEquals(3001L, result.get(0).getSubmissionId());
    }

    @Test
    void queryUserContestSubmissions_ownerDuringFreeze_returnsAllSubmissions() {
        LocalDateTime freezeTime = LocalDateTime.of(2026, 5, 3, 10, 30);
        Submission beforeFreeze = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .submitTime(freezeTime.minusMinutes(1))
                .build();
        Submission atFreeze = Submission.builder()
                .id(3002L)
                .problemId(1001L)
                .submitTime(freezeTime)
                .build();

        when(authService.isLogin()).thenReturn(true);
        when(authService.getLoginId()).thenReturn(1L);
        when(submissionQueryService.selectByContestIdAndSubmitterId(2001L, 1L)).thenReturn(List.of(beforeFreeze, atFreeze));
        when(problemQueryService.selectNamesByIds(Set.of(1001L))).thenReturn(List.of());
        when(contestProblemQueryService.selectByContestId(2001L)).thenReturn(List.of());

        List<SubmissionListItemVO> result = submitService.queryUserContestSubmissions(2001L, 1L);

        assertEquals(2, result.size());
        verify(contestQueryService, never()).select(2001L);
    }

    @Test
    void queryUserContestSubmissions_adminDuringFreeze_returnsAllSubmissions() {
        LocalDateTime freezeTime = LocalDateTime.of(2026, 5, 3, 10, 30);
        Submission beforeFreeze = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .submitTime(freezeTime.minusMinutes(1))
                .build();
        Submission atFreeze = Submission.builder()
                .id(3002L)
                .problemId(1001L)
                .submitTime(freezeTime)
                .build();

        when(authService.isLogin()).thenReturn(true);
        when(authService.getLoginId()).thenReturn(99L);
        when(authService.getRole(99L)).thenReturn("admin");
        when(submissionQueryService.selectByContestIdAndSubmitterId(2001L, 1L)).thenReturn(List.of(beforeFreeze, atFreeze));
        when(problemQueryService.selectNamesByIds(Set.of(1001L))).thenReturn(List.of());
        when(contestProblemQueryService.selectByContestId(2001L)).thenReturn(List.of());

        List<SubmissionListItemVO> result = submitService.queryUserContestSubmissions(2001L, 1L);

        assertEquals(2, result.size());
        verify(contestQueryService, never()).select(2001L);
    }

    @Test
    void queryUserContestSubmissions_afterContestEnd_returnsFrozenSubmissions() {
        LocalDateTime freezeTime = LocalDateTime.of(2026, 5, 3, 10, 30);
        Submission atFreeze = Submission.builder()
                .id(3002L)
                .problemId(1001L)
                .submitTime(freezeTime)
                .build();
        Contest contest = Contest.builder()
                .id(2001L)
                .freezeTime(freezeTime)
                .endTime(LocalDateTime.now().minusHours(1))
                .build();

        when(authService.isLogin()).thenReturn(false);
        when(submissionQueryService.selectByContestIdAndSubmitterId(2001L, 1L)).thenReturn(List.of(atFreeze));
        when(contestQueryService.select(2001L)).thenReturn(contest);
        when(problemQueryService.selectNamesByIds(Set.of(1001L))).thenReturn(List.of());
        when(contestProblemQueryService.selectByContestId(2001L)).thenReturn(List.of());

        List<SubmissionListItemVO> result = submitService.queryUserContestSubmissions(2001L, 1L);

        assertEquals(1, result.size());
        assertEquals(3002L, result.get(0).getSubmissionId());
    }

    @Test
    void queryUserSubmissions_returnsPagedProblemDisplayName() {
        LocalDateTime submitTime = LocalDateTime.now();
        Submission submission = Submission.builder()
                .id(3001L)
                .problemId(1001L)
                .submitTime(submitTime)
                .language("java")
                .status("WRONG_ANSWER")
                .timeUsedMs(64)
                .memoryUsedMb(32)
                .build();
        Problem problem = new Problem();
        problem.setId(1001L);
        problem.setName("A + B Problem");
        Page<Submission> page = new Page<>(1, 30, 1);
        page.setRecords(List.of(submission));

        when(authService.checkLogin()).thenReturn(1L);
        when(submissionQueryService.selectPageBySubmitterId(1L, 1L, 30L)).thenReturn(page);
        when(problemQueryService.selectNamesByIds(Set.of(1001L))).thenReturn(List.of(problem));

        SubmissionPageVO result = submitService.queryUserSubmissions(1L, 1L, 30L);

        assertEquals(1, result.getRecords().size());
        assertEquals("#1001. A + B Problem", result.getRecords().get(0).getProblemName());
        assertEquals("java", result.getRecords().get(0).getLanguage());
        assertEquals("WRONG_ANSWER", result.getRecords().get(0).getStatus());
        assertEquals(64, result.getRecords().get(0).getTime());
        assertEquals(32, result.getRecords().get(0).getMemory());
        assertEquals(submitTime, result.getRecords().get(0).getSubmitTime());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getCurrent());
        assertEquals(30, result.getSize());
        assertEquals(1, result.getPages());
    }

    @Test
    void queryUserSubmissions_adminCanViewOtherUserSubmissions() {
        Page<Submission> page = new Page<>(1, 30, 0);
        page.setRecords(List.of());

        when(authService.checkLogin()).thenReturn(99L);
        when(authService.getRole(99L)).thenReturn("super_admin");
        when(submissionQueryService.selectPageBySubmitterId(1L, 1L, 30L)).thenReturn(page);

        SubmissionPageVO result = submitService.queryUserSubmissions(1L, 1L, 30L);

        assertEquals(0, result.getRecords().size());
        assertEquals(0, result.getTotal());
    }

    @Test
    void queryUserSubmissions_otherUserNonAdmin_throwsBizException() {
        when(authService.checkLogin()).thenReturn(2L);
        when(authService.getRole(2L)).thenReturn("user");

        BizException ex = assertThrows(BizException.class, () -> submitService.queryUserSubmissions(1L, 1L, 30L));

        assertEquals(Code.FORBIDDEN, ex.getCode());
        verify(submissionQueryService, never()).selectPageBySubmitterId(1L, 1L, 30L);
    }

    @Test
    void queryUserSubmissions_invalidPageRequest_throwsBizException() {
        BizException ex = assertThrows(BizException.class, () -> submitService.queryUserSubmissions(1L, 0L, 30L));

        assertEquals(Code.SUBMIT_REQUEST_INVALID, ex.getCode());
        verifyNoInteractions(problemQueryService, contestProblemQueryService, minioService);
    }
}
