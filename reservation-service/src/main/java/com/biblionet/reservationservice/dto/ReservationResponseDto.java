package com.biblionet.reservationservice.dto;

import com.biblionet.reservationservice.entity.ReservationStatus;

import java.time.LocalDateTime;

public class ReservationResponseDto {

    private Long id;
    private Long memberId;
    private Long bookId;
    private LocalDateTime requestDate;
    private ReservationStatus status;

    public ReservationResponseDto() {
    }

    public ReservationResponseDto(Long id, Long memberId, Long bookId, LocalDateTime requestDate,
                                  ReservationStatus status) {
        this.id = id;
        this.memberId = memberId;
        this.bookId = bookId;
        this.requestDate = requestDate;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

}
