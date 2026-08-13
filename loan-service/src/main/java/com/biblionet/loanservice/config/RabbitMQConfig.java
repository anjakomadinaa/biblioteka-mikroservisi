package com.biblionet.loanservice.config;

import com.biblionet.loanservice.dto.LoanEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String LOAN_EXCHANGE = "loan.events.exchange";

    /** Logicko ime tipa u __TypeId__ headeru - deo ugovora sa potrosacima. */
    public static final String LOAN_EVENT_TYPE_ID = "loanEvent";

    @Bean
    public TopicExchange loanEventsExchange() {
        return new TopicExchange(LOAN_EXCHANGE);
    }

    /**
     * Koristi se ObjectMapper koji je Spring Boot vec konfigurisao, da bi datumi u
     * eventovima isli kao ISO-8601 string, isto kao u REST odgovorima. Rucno napravljen
     * ObjectMapper bi ih serijalizovao kao niz brojeva.
     * <p>
     * Type mapper je podesen da u __TypeId__ upisuje logicko ime ("loanEvent") umesto
     * punog imena klase. Podrazumevano ponasanje bi objavilo com.biblionet.loanservice.dto.LoanEvent,
     * sto potrosace vezuje za nasu strukturu paketa i puca kod njih sa ClassNotFoundException.
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setIdClassMapping(Map.of(LOAN_EVENT_TYPE_ID, LoanEvent.class));
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

}
