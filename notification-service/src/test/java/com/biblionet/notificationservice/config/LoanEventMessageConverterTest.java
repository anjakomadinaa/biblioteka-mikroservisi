package com.biblionet.notificationservice.config;

import com.biblionet.notificationservice.dto.LoanEvent;
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
 * Cuva ugovor sa loan-service-om: poruka nosi __TypeId__ sa klasom koja ovde ne postoji,
 * pa konverter mora da odredi tip iz potpisa listener metode. Bez toga ceo asinhroni
 * lanac puca sa ClassNotFoundException, a to nijedan unit test listenera ne bi uhvatio.
 */
@SpringBootTest
class LoanEventMessageConverterTest {

    @Autowired
    private MessageConverter messageConverter;

    @Test
    void deserializesEventWithLogicalTypeId() {
        LoanEvent event = convertWithTypeId("loanEvent");

        assertThat(event.getEventType()).isEqualTo("LOAN_CREATED");
        assertThat(event.getLoanId()).isEqualTo(1L);
        assertThat(event.getMemberId()).isEqualTo(5L);
        assertThat(event.getBookId()).isEqualTo(7L);
        assertThat(event.getTimestamp()).isEqualTo(LocalDateTime.of(2026, 8, 13, 18, 30));
    }

    @Test
    void deserializesEventEvenWhenTypeIdIsAnUnknownClassName() {
        // otpornost na starije/drugacije podesene producente
        LoanEvent event = convertWithTypeId("com.biblionet.loanservice.dto.LoanEvent");

        assertThat(event.getEventType()).isEqualTo("LOAN_CREATED");
        assertThat(event.getLoanId()).isEqualTo(1L);
    }

    private LoanEvent convertWithTypeId(String typeId) {
        String json = "{\"eventType\":\"LOAN_CREATED\",\"loanId\":1,\"memberId\":5,\"bookId\":7,"
                + "\"timestamp\":\"2026-08-13T18:30:00\"}";

        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        properties.setHeader("__TypeId__", typeId);
        properties.setInferredArgumentType(LoanEvent.class);

        Object converted = messageConverter.fromMessage(
                new Message(json.getBytes(StandardCharsets.UTF_8), properties));

        assertThat(converted).isInstanceOf(LoanEvent.class);
        return (LoanEvent) converted;
    }

}
