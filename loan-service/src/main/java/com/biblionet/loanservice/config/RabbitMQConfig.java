package com.biblionet.loanservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String LOAN_EXCHANGE = "loan.events.exchange";

    @Bean
    public TopicExchange loanEventsExchange() {
        return new TopicExchange(LOAN_EXCHANGE);
    }

    /**
     * Koristi se ObjectMapper koji je Spring Boot vec konfigurisao, da bi datumi u
     * eventovima isli kao ISO-8601 string, isto kao u REST odgovorima. Rucno napravljen
     * ObjectMapper bi ih serijalizovao kao niz brojeva.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

}
