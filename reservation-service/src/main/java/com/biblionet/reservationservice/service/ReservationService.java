package com.biblionet.reservationservice.service;

import com.biblionet.reservationservice.dto.ReservationRequestDto;
import com.biblionet.reservationservice.dto.ReservationResponseDto;

import java.util.List;
import java.util.Optional;

public interface ReservationService {

    ReservationResponseDto createReservation(ReservationRequestDto request);

    List<ReservationResponseDto> getAllReservations();

    ReservationResponseDto getReservationById(Long id);

    ReservationResponseDto cancelReservation(Long id);

    /**
     * Pomera red cekanja za vracenu knjigu: prvi clan na redu dobija status NOTIFIED.
     *
     * @return obavesten clan, ili prazno ako niko ne ceka na tu knjigu
     */
    Optional<ReservationResponseDto> notifyNextInQueue(Long bookId);

}
