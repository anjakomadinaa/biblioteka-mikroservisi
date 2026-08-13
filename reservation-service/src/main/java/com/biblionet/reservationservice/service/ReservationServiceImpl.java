package com.biblionet.reservationservice.service;

import com.biblionet.reservationservice.dto.ReservationRequestDto;
import com.biblionet.reservationservice.dto.ReservationResponseDto;
import com.biblionet.reservationservice.entity.Reservation;
import com.biblionet.reservationservice.entity.ReservationStatus;
import com.biblionet.reservationservice.exception.DuplicateReservationException;
import com.biblionet.reservationservice.exception.ResourceNotFoundException;
import com.biblionet.reservationservice.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationServiceImpl(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    @Transactional
    public ReservationResponseDto createReservation(ReservationRequestDto request) {
        boolean alreadyWaiting = reservationRepository.existsByMemberIdAndBookIdAndStatus(
                request.getMemberId(), request.getBookId(), ReservationStatus.WAITING);
        if (alreadyWaiting) {
            throw new DuplicateReservationException(
                    "Član #" + request.getMemberId() + " već čeka na knjigu #" + request.getBookId());
        }

        Reservation reservation = new Reservation(request.getMemberId(), request.getBookId());
        return toResponse(reservationRepository.save(reservation));
    }

    @Override
    public List<ReservationResponseDto> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ReservationResponseDto getReservationById(Long id) {
        return toResponse(findReservationOrThrow(id));
    }

    /**
     * Otkazivanje je "meko" - rezervacija ostaje u bazi sa statusom CANCELLED umesto
     * da se brise. Tako ostaje trag ko je i kada bio u redu, a otkazana rezervacija
     * vise ne ucestvuje u FIFO redosledu jer se trazi samo status WAITING.
     */
    @Override
    @Transactional
    public ReservationResponseDto cancelReservation(Long id) {
        Reservation reservation = findReservationOrThrow(id);
        reservation.cancel();
        return toResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public Optional<ReservationResponseDto> notifyNextInQueue(Long bookId) {
        List<Reservation> queue = reservationRepository
                .findByBookIdAndStatusOrderByRequestDateAsc(bookId, ReservationStatus.WAITING);

        if (queue.isEmpty()) {
            return Optional.empty();
        }

        Reservation first = queue.get(0);
        first.markNotified();
        return Optional.of(toResponse(reservationRepository.save(first)));
    }

    private Reservation findReservationOrThrow(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rezervacija sa ID " + id + " nije pronađena"));
    }

    private ReservationResponseDto toResponse(Reservation reservation) {
        return new ReservationResponseDto(
                reservation.getId(),
                reservation.getMemberId(),
                reservation.getBookId(),
                reservation.getRequestDate(),
                reservation.getStatus()
        );
    }

}
