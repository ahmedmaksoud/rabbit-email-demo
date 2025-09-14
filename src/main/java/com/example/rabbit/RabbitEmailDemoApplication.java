package com.example.rabbit;

import com.example.rabbit.model.WorkRequest;
import com.example.rabbit.service.PublisherService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RabbitEmailDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitEmailDemoApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(PublisherService publisher) {
        return args -> {
            var req = new WorkRequest("JOB-EMAIL-1", "Hello with email!", "user@example.com");
            String corrId = publisher.publishWork(req);
            System.out.println("[MAIN] Published with corrId=" + corrId);
        };
    }
}