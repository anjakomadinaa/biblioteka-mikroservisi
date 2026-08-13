package com.biblionet.reservationservice.listener;

import com.biblionet.reservationservice.config.RabbitMQConfig;
import com.biblionet.reservationservice.dto.LoanEvent;
import com.biblionet.reservationservice.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BookReturnedListener {

    private static final Logger log = LoggerFactory.getLogger(BookReturnedListener.class);

    private final ReservationService reservationService;

    public BookReturnedListener(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @RabbitListener(queues = RabbitMQConfig.RESERVATION_QUEUE)
    public void onBookReturned(LoanEvent event) {
        reservationService.notifyNextInQueue(event.getBookId())
                .ifPresentOrElse(
                        reservation -> log.info("Knjiga {} je vraćena - obavešten član {} (rezervacija {})",
                                event.getBookId(), reservation.getMemberId(), reservation.getId()),
                        () -> log.info("Knjiga {} je vraćena - niko ne čeka u redu", event.getBookId()));
    }

}
