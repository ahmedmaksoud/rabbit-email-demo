package com.example.rabbit.service;

import com.example.rabbit.model.WorkConfirmation;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReplyListener {

    @RabbitListener(queues = "reply.queue")
    public void onReply(WorkConfirmation confirmation, Message raw) {
        String correlationId = raw.getMessageProperties().getCorrelationId();
        System.out.println("[CALLBACK] corrId=" + correlationId +
                " jobId=" + confirmation.jobId() +
                " status=" + confirmation.status() +
                " details=" + confirmation.details());
    }
}