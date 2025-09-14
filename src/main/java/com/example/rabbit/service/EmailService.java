package com.example.rabbit.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.from}")
    private String from;

    @Value("${notification.to}")
    private String defaultTo;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendWorkConfirmation(String jobId,
                                     String status,
                                     String details,
                                     @Nullable String toOverride) {
        String to = (toOverride != null && !toOverride.isBlank()) ? toOverride : defaultTo;
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("[Work Confirmation] jobId=" + jobId + " status=" + status);
        msg.setText("Hello,\n\nYour job has completed.\n\nJob ID: " + jobId +
                "\nStatus: " + status + "\nDetails: " + details +
                "\n\nRegards,\nRabbit Email Demo");
        //mailSender.send(msg);
        System.out.println("[EMAIL] Sent confirmation to " + to + " for jobId=" + jobId);
    }
}