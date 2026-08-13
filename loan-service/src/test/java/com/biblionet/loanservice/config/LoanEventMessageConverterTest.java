package com.biblionet.loanservice.config;

import com.biblionet.loanservice.dto.LoanEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cuva ugovor prema potrosacima dogadjaja (notification-service, reservation-service).
 */
@SpringBootTest
class LoanEventMessageConverterTest {

    @Autowired
    private MessageConverter messageConverter;

    @Test
    void publishesLogicalTypeIdInsteadOfInternalClassName() {
        Message message = messageConverter.toMessage(sampleEvent(), new MessageProperties());

        // puno ime klase bi vezalo potrosace za nasu strukturu paketa
        assertThat(message.getMessageProperties().getHeaders().get("__TypeId__"))
                .isEqualTo(RabbitMQConfig.LOAN_EVENT_TYPE_ID);
    }

    @Test
    void publishesTimestampAsIsoStringLikeRestApi() {
        Message message = messageConverter.toMessage(sampleEvent(), new MessageProperties());

        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        assertThat(body).contains("\"timestamp\":\"2026-08-13T18:30:00\"");
        assertThat(body).contains("\"eventType\":\"LOAN_CREATED\"");
    }

    private LoanEvent sampleEvent() {
        return new LoanEvent("LOAN_CREATED", 1L, 5L, 7L, LocalDateTime.of(2026, 8, 13, 18, 30, 0));
    }

}
