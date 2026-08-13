package com.biblionet.notificationservice.listener;

import com.biblionet.notificationservice.config.RabbitMQConfig;
import com.biblionet.notificationservice.dto.LoanEvent;
import com.biblionet.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class LoanEventListener {

    private static final Logger log = LoggerFactory.getLogger(LoanEventListener.class);

    private final NotificationService notificationService;

    public LoanEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void onLoanEvent(LoanEvent event) {
        try {
            notificationService.createFromEvent(event);
            log.info("Obradjen dogadjaj {} za pozajmicu {}", event.getEventType(), event.getLoanId());
        } catch (IllegalArgumentException ex) {
            // Nepoznat tip dogadjaja se odbacuje - ponovno slanje bi se vrtelo u krug.
            // Ostale greske (npr. baza nedostupna) se namerno propagiraju radi retry-ja.
            log.warn("Odbacen dogadjaj nepoznatog tipa: {}", event.getEventType());
        }
    }

}
