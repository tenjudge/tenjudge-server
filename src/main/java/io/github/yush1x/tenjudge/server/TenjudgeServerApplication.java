package io.github.yush1x.tenjudge.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class TenjudgeServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenjudgeServerApplication.class, args);
    }

}
