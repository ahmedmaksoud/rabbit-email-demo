package com.example.rabbit.service;

import com.example.rabbit.RabbitConfig;
import com.example.rabbit.model.WorkRequest;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class PublisherService {

    private final RabbitTemplate template;

    public PublisherService(RabbitTemplate template) {
        this.template = template;
    }

    public String publishWork(WorkRequest request) {
        String correlationId = UUID.randomUUID().toString();
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        props.setReplyToAddress(new Address(RabbitConfig.EXCHANGE + "/" + RabbitConfig.REPLY_RK));
        props.setCorrelationId(correlationId);
        /// write on disk
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        Message msg = template.getMessageConverter().toMessage(request, props);
        CorrelationData cd = new CorrelationData(correlationId);
        template.send(RabbitConfig.EXCHANGE, RabbitConfig.WORK_RK, msg, cd);
        System.out.println("[PUBLISHER] Sent job " + request.jobId() + " corrId=" + correlationId);
        return correlationId;
    }
}