package com.biblionet.reservationservice.config;

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
    public static final String RESERVATION_QUEUE = "reservation.queue";

    /**
     * Tacan routing key, ne "loan.*" - red cekanja se pomera samo kad se knjiga vrati.
     * Dogadjaji o kreiranju pozajmice ovaj servis ne zanimaju.
     */
    public static final String BOOK_RETURNED_ROUTING_KEY = "loan.returned";

    /** Deklarisan i ovde jer je deklaracija u RabbitMQ-u idempotentna. */
    @Bean
    public TopicExchange loanEventsExchange() {
        return new TopicExchange(LOAN_EXCHANGE);
    }

    @Bean
    public Queue reservationQueue() {
        return QueueBuilder.durable(RESERVATION_QUEUE).build();
    }

    @Bean
    public Binding reservationBinding(Queue reservationQueue, TopicExchange loanEventsExchange) {
        return BindingBuilder.bind(reservationQueue).to(loanEventsExchange).with(BOOK_RETURNED_ROUTING_KEY);
    }

    /**
     * Tip poruke se odredjuje iz potpisa @RabbitListener metode (INFERRED), a __TypeId__
     * header se ignorise - producent tamo salje svoje logicko ime tipa, koje ovde ne
     * odgovara nijednoj klasi.
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
