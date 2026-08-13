package com.biblionet.reservationservice.listener;

import com.biblionet.reservationservice.dto.LoanEvent;
import com.biblionet.reservationservice.entity.Reservation;
import com.biblionet.reservationservice.entity.ReservationStatus;
import com.biblionet.reservationservice.repository.ReservationRepository;
import com.biblionet.reservationservice.service.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testira se kroz pravu bazu, jer je FIFO redosled osobina upita
 * (findByBookIdAndStatusOrderByRequestDateAsc), a ne koda koji se moze mokovati.
 */
@DataJpaTest
class BookReturnedListenerTest {

    private static final Long BOOK_ID = 7L;
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 8, 13, 10, 0);

    @Autowired
    private ReservationRepository reservationRepository;

    private BookReturnedListener listener;

    @BeforeEach
    void setUp() {
        listener = new BookReturnedListener(new ReservationServiceImpl(reservationRepository));
    }

    @Test
    void oldestWaitingReservationIsNotifiedAndOthersKeepWaiting() {
        Reservation second = givenReservation(2L, BOOK_ID, ReservationStatus.WAITING, BASE_TIME.plusHours(2));
        Reservation first = givenReservation(1L, BOOK_ID, ReservationStatus.WAITING, BASE_TIME);
        Reservation third = givenReservation(3L, BOOK_ID, ReservationStatus.WAITING, BASE_TIME.plusHours(5));

        listener.onBookReturned(bookReturnedEvent(BOOK_ID));

        // clan 1 je prvi rezervisao, iako je u bazi upisan drugi po redu
        assertThat(statusOf(first)).isEqualTo(ReservationStatus.NOTIFIED);
        assertThat(statusOf(second)).isEqualTo(ReservationStatus.WAITING);
        assertThat(statusOf(third)).isEqualTo(ReservationStatus.WAITING);
    }

    @Test
    void cancelledReservationsAreSkippedEvenIfOlder() {
        Reservation cancelled = givenReservation(1L, BOOK_ID, ReservationStatus.CANCELLED, BASE_TIME);
        Reservation waiting = givenReservation(2L, BOOK_ID, ReservationStatus.WAITING, BASE_TIME.plusHours(1));

        listener.onBookReturned(bookReturnedEvent(BOOK_ID));

        assertThat(statusOf(cancelled)).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(statusOf(waiting)).isEqualTo(ReservationStatus.NOTIFIED);
    }

    @Test
    void alreadyNotifiedReservationIsNotPickedAgain() {
        Reservation notified = givenReservation(1L, BOOK_ID, ReservationStatus.NOTIFIED, BASE_TIME);
        Reservation waiting = givenReservation(2L, BOOK_ID, ReservationStatus.WAITING, BASE_TIME.plusHours(1));

        listener.onBookReturned(bookReturnedEvent(BOOK_ID));

        assertThat(statusOf(notified)).isEqualTo(ReservationStatus.NOTIFIED);
        assertThat(statusOf(waiting)).isEqualTo(ReservationStatus.NOTIFIED);
    }

    @Test
    void reservationsForOtherBooksAreUntouched() {
        Reservation otherBook = givenReservation(1L, 99L, ReservationStatus.WAITING, BASE_TIME);
        Reservation thisBook = givenReservation(2L, BOOK_ID, ReservationStatus.WAITING, BASE_TIME.plusHours(1));

        listener.onBookReturned(bookReturnedEvent(BOOK_ID));

        assertThat(statusOf(otherBook)).isEqualTo(ReservationStatus.WAITING);
        assertThat(statusOf(thisBook)).isEqualTo(ReservationStatus.NOTIFIED);
    }

    @Test
    void nothingHappensWhenNobodyIsWaiting() {
        Reservation cancelled = givenReservation(1L, BOOK_ID, ReservationStatus.CANCELLED, BASE_TIME);

        listener.onBookReturned(bookReturnedEvent(BOOK_ID));

        assertThat(statusOf(cancelled)).isEqualTo(ReservationStatus.CANCELLED);
    }

    private Reservation givenReservation(Long memberId, Long bookId, ReservationStatus status,
                                         LocalDateTime requestDate) {
        Reservation reservation = new Reservation(memberId, bookId);
        reservation.setRequestDate(requestDate);
        reservation.setStatus(status);
        return reservationRepository.saveAndFlush(reservation);
    }

    private ReservationStatus statusOf(Reservation reservation) {
        return reservationRepository.findById(reservation.getId()).orElseThrow().getStatus();
    }

    private LoanEvent bookReturnedEvent(Long bookId) {
        return new LoanEvent("LOAN_RETURNED", 100L, 55L, bookId, LocalDateTime.now());
    }

}
