package com.example.rabbit;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "app.direct";
    public static final String WORK_QUEUE = "work.queue";
    public static final String REPLY_QUEUE = "reply.queue";
    public static final String WORK_RK = "work";
    public static final String REPLY_RK = "reply";

    @Bean
    public DirectExchange appExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue workQueue() {
        return QueueBuilder.durable(WORK_QUEUE).build();
    }

    @Bean
    public Queue replyQueue() {
        return QueueBuilder.durable(REPLY_QUEUE).build();
    }

    @Bean
    public Binding workBinding() {
        return BindingBuilder.bind(workQueue()).to(appExchange()).with(WORK_RK);
    }

    @Bean
    public Binding replyBinding() {
        return BindingBuilder.bind(replyQueue()).to(appExchange()).with(REPLY_RK);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(messageConverter());
        template.setConfirmCallback((CorrelationData correlation, boolean ack, String cause) -> {
            String id = correlation != null ? correlation.getId() : "null";
            if (ack) {
                System.out.println("[BROKER-CONFIRM] Ack for correlationId=" + id);
            } else {
                System.out.println("[BROKER-CONFIRM] Nack for correlationId=" + id + " cause=" + cause);
            }
        });
        template.setReturnsCallback(returned -> {
            System.out.println("[RETURNED] " + returned);
        });
        return template;
    }
}