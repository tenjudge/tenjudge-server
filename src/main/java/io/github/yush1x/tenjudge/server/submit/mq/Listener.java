package io.github.yush1x.tenjudge.server.submit.mq;

import io.github.yush1x.tenjudge.server.contest.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Listener {
    private final BoardService boardService;

    @RabbitListener(queues = "tenjudge.judge.complete.queue")
    public void receiveMessage(Long submissionId) {
        boardService.handleJudgeResult(submissionId);
    }
}
