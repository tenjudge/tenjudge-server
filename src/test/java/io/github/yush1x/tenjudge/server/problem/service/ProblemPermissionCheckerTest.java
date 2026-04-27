package io.github.yush1x.tenjudge.server.problem.service;

import io.github.yush1x.tenjudge.server.auth.service.AuthService;
import io.github.yush1x.tenjudge.server.common.Code;
import io.github.yush1x.tenjudge.server.contest.entity.Contest;
import io.github.yush1x.tenjudge.server.contest.entity.ContestParticipant;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestParticipantQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestProblemQueryService;
import io.github.yush1x.tenjudge.server.contest.persistence.ContestQueryService;
import io.github.yush1x.tenjudge.server.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProblemPermissionCheckerTest {

    @Mock
    private AuthService authService;

    @Mock
    private ContestQueryService contestQueryService;

    @Mock
    private ContestProblemQueryService contestProblemQueryService;

    @Mock
    private ContestParticipantQueryService contestParticipantQueryService;

    @InjectMocks
    private ProblemPermissionChecker problemPermissionChecker;

    @BeforeEach
    void setUp() {
        when(authService.checkLogin()).thenReturn(1L);
        when(authService.getRole(1L)).thenReturn("user");
    }

    @Test
    void checkAccessPermission_adminAgentBypassesAllChecks() {
        when(authService.getRole(1L)).thenReturn("admin");

        assertDoesNotThrow(() -> problemPermissionChecker.checkAccessPermission(100L, "private", 10L, true));

        verify(contestQueryService, never()).select(10L);
        verifyNoInteractions(contestProblemQueryService, contestParticipantQueryService);
    }

    @Test
    void checkAccessPermission_publicProblem_allowsDirectAccess() {
        assertDoesNotThrow(() -> problemPermissionChecker.checkAccessPermission(100L, "public", null, true));

        verifyNoInteractions(contestQueryService, contestProblemQueryService, contestParticipantQueryService);
    }

    @Test
    void checkAccessPermission_privateProblemDuringContest_allowsUserWithoutRegistration() {
        when(contestQueryService.select(10L)).thenReturn(runningContest());
        when(contestProblemQueryService.exists(10L, 100L)).thenReturn(true);

        assertDoesNotThrow(() -> problemPermissionChecker.checkAccessPermission(100L, "private", 10L, false));

        verify(contestQueryService).select(10L);
        verify(contestProblemQueryService).exists(10L, 100L);
        verifyNoInteractions(contestParticipantQueryService);
    }

    @Test
    void checkAccessPermission_privateProblemAgent_throwsForbidden() {
        when(contestQueryService.select(10L)).thenReturn(runningContest());
        when(contestProblemQueryService.exists(10L, 100L)).thenReturn(true);

        BizException ex = assertThrows(
                BizException.class,
                () -> problemPermissionChecker.checkAccessPermission(100L, "private", 10L, true)
        );

        assertEquals(Code.FORBIDDEN, ex.getCode());
    }

    @Test
    void checkSubmitPermission_privateProblemWithoutRegistration_throwsForbidden() {
        when(contestQueryService.select(10L)).thenReturn(runningContest());
        when(contestProblemQueryService.exists(10L, 100L)).thenReturn(true);
        when(contestParticipantQueryService.select(10L, 1L)).thenReturn(null);

        BizException ex = assertThrows(
                BizException.class,
                () -> problemPermissionChecker.checkSubmitPermission(100L, "private", 10L, false)
        );

        assertEquals(Code.FORBIDDEN, ex.getCode());
    }

    @Test
    void checkSubmitPermission_privateProblemWithRegistration_allowsSubmit() {
        when(contestQueryService.select(10L)).thenReturn(runningContest());
        when(contestProblemQueryService.exists(10L, 100L)).thenReturn(true);
        when(contestParticipantQueryService.select(10L, 1L)).thenReturn(new ContestParticipant());

        assertDoesNotThrow(() -> problemPermissionChecker.checkSubmitPermission(100L, "private", 10L, false));
    }

    private Contest runningContest() {
        Contest contest = new Contest();
        contest.setId(10L);
        contest.setStartTime(LocalDateTime.now().minusMinutes(30));
        contest.setEndTime(LocalDateTime.now().plusMinutes(30));
        return contest;
    }
}
