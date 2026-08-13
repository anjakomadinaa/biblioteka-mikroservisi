package com.biblionet.loanservice.dto;

import jakarta.validation.constraints.NotNull;

public class LoanRequestDto {

    @NotNull(message = "ID člana je obavezan")
    private Long memberId;

    @NotNull(message = "ID knjige je obavezan")
    private Long bookId;

    public LoanRequestDto() {
    }

    public LoanRequestDto(Long memberId, Long bookId) {
        this.memberId = memberId;
        this.bookId = bookId;
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

}
