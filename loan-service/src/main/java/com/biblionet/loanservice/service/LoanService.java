package com.biblionet.loanservice.service;

import com.biblionet.loanservice.dto.LoanRequestDto;
import com.biblionet.loanservice.dto.LoanResponseDto;

import java.util.List;

public interface LoanService {

    LoanResponseDto createLoan(LoanRequestDto request);

    List<LoanResponseDto> getAllLoans();

    LoanResponseDto getLoanById(Long id);

    LoanResponseDto returnLoan(Long id);

}
