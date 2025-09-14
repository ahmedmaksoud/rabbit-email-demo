package com.example.rabbit.service;

import com.example.rabbit.model.WorkConfirmation;
import com.example.rabbit.model.WorkRequest;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class WorkerListener {

    private final RabbitTemplate template;
    private final EmailService emailService;
    private final DedupService dedup;

    public WorkerListener(RabbitTemplate template, EmailService emailService, DedupService dedup) {
        this.template = template;
        this.emailService = emailService;
        this.dedup = dedup;
    }

    @RabbitListener(queues = "work.queue")
    public void onWork(WorkRequest req, Message raw) {
        try {

            var correlationId = raw.getMessageProperties().getCorrelationId();
            //demo to make sure message deduplicate
            if (!dedup.firstTimeSeen(correlationId)) {
                System.out.println("[WORKER] Duplicate detected. Skip processing for key=" + correlationId);
                return; // don't send email or callback again
            }
            System.out.println("[WORKER] Processing jobId=" + req.jobId() + " payload=" + req.payload());
            String status = "SUCCESS";
            String details = "Processed payload length=" + req.payload().length();
            emailService.sendWorkConfirmation(req.jobId(), status, details, req.notifyEmail());
            var replyTo = raw.getMessageProperties().getReplyToAddress();

            WorkConfirmation confirmation = new WorkConfirmation(req.jobId(), status, details);
            if (replyTo != null) {
                template.convertAndSend(
                        replyTo.getExchangeName(),
                        replyTo.getRoutingKey(),
                        confirmation,
                        m -> {
                            m.getMessageProperties().setCorrelationId(correlationId);
                            return m;
                        });
                System.out.println("[WORKER] Replied for corrId=" + correlationId);
            } else {
                System.out.println("[WORKER] No replyTo set; skipping callback queue reply");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}