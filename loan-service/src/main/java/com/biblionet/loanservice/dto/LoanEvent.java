package com.biblionet.loanservice.dto;

import java.time.LocalDateTime;

public class LoanEvent {

    private String eventType;
    private Long loanId;
    private Long memberId;
    private Long bookId;
    private LocalDateTime timestamp;

    public LoanEvent() {
    }

    public LoanEvent(String eventType, Long loanId, Long memberId, Long bookId, LocalDateTime timestamp) {
        this.eventType = eventType;
        this.loanId = loanId;
        this.memberId = memberId;
        this.bookId = bookId;
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

}
