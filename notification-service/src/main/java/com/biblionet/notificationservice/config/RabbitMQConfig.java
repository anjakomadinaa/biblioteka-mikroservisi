package com.biblionet.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String LOAN_EXCHANGE = "loan.events.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String LOAN_ROUTING_PATTERN = "loan.*";

    /** Deklarisan i ovde jer je deklaracija u RabbitMQ-u idempotentna. */
    @Bean
    public TopicExchange loanEventsExchange() {
        return new TopicExchange(LOAN_EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange loanEventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(loanEventsExchange).with(LOAN_ROUTING_PATTERN);
    }

    /**
     * VAZNO: loan-service u poruku upisuje __TypeId__ header sa svojom klasom
     * (com.biblionet.loanservice.dto.LoanEvent), koja ovde ne postoji. Zato se tip
     * odredjuje iz potpisa @RabbitListener metode (INFERRED), a header se ignorise -
     * bez ovoga deserijalizacija puca sa ClassNotFoundException.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("com.biblionet.*");
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

}
