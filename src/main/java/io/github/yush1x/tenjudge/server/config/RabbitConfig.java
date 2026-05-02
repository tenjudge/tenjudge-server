package io.github.yush1x.tenjudge.server.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // 1. 声明交换机
    @Bean
    public DirectExchange judgeExchange() {
        return new DirectExchange("tenjudge.judge.exchange", true, false);
    }

    // 2. 声明队列
    @Bean
    public Queue judgeQueue() {
        return new Queue("tenjudge.judge.complete.queue", true);
    }

    // 3. 声明绑定关系
    @Bean
    public Binding binding(Queue judgeQueue, DirectExchange judgeExchange) {
        return BindingBuilder.bind(judgeQueue).to(judgeExchange).with("complete");
    }
}

