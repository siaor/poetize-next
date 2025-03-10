package com.siaor.poetize.next;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class PoetizeNextApplication {

    public static void main(String[] args) {
        SpringApplication.run(PoetizeNextApplication.class, args);
    }

}
