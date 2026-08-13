package com.biblionet.loanservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "loans")
public class Loan {

    private static final int LOAN_PERIOD_DAYS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private LocalDate loanDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    protected Loan() {
    }

    public Loan(Long memberId, Long bookId) {
        this.memberId = memberId;
        this.bookId = bookId;
        this.loanDate = LocalDate.now();
        this.dueDate = this.loanDate.plusDays(LOAN_PERIOD_DAYS);
        this.status = LoanStatus.ACTIVE;
    }

    public void markReturned() {
        this.returnDate = LocalDate.now();
        this.status = LoanStatus.RETURNED;
    }

    /** Kompenzacija kada vracanje ne uspe da se propagira na book-service. */
    public void revertReturn() {
        this.returnDate = null;
        this.status = LoanStatus.ACTIVE;
    }

    public boolean isReturned() {
        return status == LoanStatus.RETURNED;
    }

    public Long getId() {
        return id;
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

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

}
