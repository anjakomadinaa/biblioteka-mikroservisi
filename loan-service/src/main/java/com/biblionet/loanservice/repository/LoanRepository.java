package com.biblionet.loanservice.repository;

import com.biblionet.loanservice.entity.Loan;
import com.biblionet.loanservice.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    boolean existsByBookIdAndStatus(Long bookId, LoanStatus status);

}
