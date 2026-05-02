package io.github.yush1x.tenjudge.server.submit.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Listener {
    @RabbitListener(queues = "tenjudge.judge.complete.queue")
    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);
    }
}
