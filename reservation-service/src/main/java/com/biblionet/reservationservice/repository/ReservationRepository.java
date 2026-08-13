package com.biblionet.reservationservice.repository;

import com.biblionet.reservationservice.entity.Reservation;
import com.biblionet.reservationservice.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** FIFO red cekanja - najstariji zahtev je prvi na redu. */
    List<Reservation> findByBookIdAndStatusOrderByRequestDateAsc(Long bookId, ReservationStatus status);

    boolean existsByMemberIdAndBookIdAndStatus(Long memberId, Long bookId, ReservationStatus status);

}
