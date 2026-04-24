package io.github.yush1x.tenjudge.server.submit.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Producer {

    private final RabbitTemplate rabbitTemplate;

    public void send(Long submissionId) {
        rabbitTemplate.convertAndSend("tenjudge.judge.exchange", "submit", submissionId);
    }
}
