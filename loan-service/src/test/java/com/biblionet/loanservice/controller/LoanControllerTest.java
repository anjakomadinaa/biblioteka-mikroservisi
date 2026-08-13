package com.biblionet.loanservice.controller;

import com.biblionet.loanservice.dto.LoanRequestDto;
import com.biblionet.loanservice.dto.LoanResponseDto;
import com.biblionet.loanservice.entity.LoanStatus;
import com.biblionet.loanservice.exception.BookNotAvailableException;
import com.biblionet.loanservice.exception.InvalidLoanStateException;
import com.biblionet.loanservice.exception.ResourceNotFoundException;
import com.biblionet.loanservice.exception.ServiceUnavailableException;
import com.biblionet.loanservice.service.LoanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanController.class)
class LoanControllerTest {

    private static final LocalDate LOAN_DATE = LocalDate.of(2026, 8, 13);
    private static final LocalDate DUE_DATE = LOAN_DATE.plusDays(14);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoanService loanService;

    @Test
    void createLoanReturns201WithActiveLoan() throws Exception {
        given(loanService.createLoan(any(LoanRequestDto.class))).willReturn(
                new LoanResponseDto(1L, 5L, 7L, LOAN_DATE, DUE_DATE, null, LoanStatus.ACTIVE));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 7L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.memberId").value(5))
                .andExpect(jsonPath("$.bookId").value(7))
                .andExpect(jsonPath("$.loanDate").value("2026-08-13"))
                .andExpect(jsonPath("$.dueDate").value("2026-08-27"))
                .andExpect(jsonPath("$.returnDate").doesNotExist())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createLoanWithMissingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/loans"))
                .andExpect(jsonPath("$.errors.memberId").exists())
                .andExpect(jsonPath("$.errors.bookId").exists());

        verifyNoInteractions(loanService);
    }

    @Test
    void createLoanReturns404WhenMemberMissing() throws Exception {
        given(loanService.createLoan(any(LoanRequestDto.class)))
                .willThrow(new ResourceNotFoundException("Član sa ID 99 nije pronađen"));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(99L, 7L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Član sa ID 99 nije pronađen"));
    }

    @Test
    void createLoanReturns409WhenBookNotAvailable() throws Exception {
        given(loanService.createLoan(any(LoanRequestDto.class)))
                .willThrow(new BookNotAvailableException("Knjiga sa ID 7 trenutno nije dostupna"));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 7L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Knjiga sa ID 7 trenutno nije dostupna"))
                .andExpect(jsonPath("$.path").value("/loans"));
    }

    @Test
    void getAllLoansReturnsList() throws Exception {
        given(loanService.getAllLoans()).willReturn(List.of(
                new LoanResponseDto(1L, 5L, 7L, LOAN_DATE, DUE_DATE, null, LoanStatus.ACTIVE),
                new LoanResponseDto(2L, 6L, 8L, LOAN_DATE, DUE_DATE, LOAN_DATE, LoanStatus.RETURNED)
        ));

        mockMvc.perform(get("/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].status").value("RETURNED"));
    }

    @Test
    void getLoanByIdReturnsLoan() throws Exception {
        given(loanService.getLoanById(1L)).willReturn(
                new LoanResponseDto(1L, 5L, 7L, LOAN_DATE, DUE_DATE, null, LoanStatus.ACTIVE));

        mockMvc.perform(get("/loans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getLoanByIdReturns404WhenMissing() throws Exception {
        given(loanService.getLoanById(99L))
                .willThrow(new ResourceNotFoundException("Pozajmica sa ID 99 nije pronađena"));

        mockMvc.perform(get("/loans/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Pozajmica sa ID 99 nije pronađena"))
                .andExpect(jsonPath("$.path").value("/loans/99"));
    }

    @Test
    void returnLoanMarksLoanAsReturned() throws Exception {
        given(loanService.returnLoan(1L)).willReturn(
                new LoanResponseDto(1L, 5L, 7L, LOAN_DATE, DUE_DATE, LOAN_DATE.plusDays(3), LoanStatus.RETURNED));

        mockMvc.perform(put("/loans/1/return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.returnDate").value("2026-08-16"))
                .andExpect(jsonPath("$.status").value("RETURNED"));

        verify(loanService).returnLoan(1L);
    }

    @Test
    void returnLoanReturns409WhenAlreadyReturned() throws Exception {
        given(loanService.returnLoan(1L))
                .willThrow(new InvalidLoanStateException("Pozajmica sa ID 1 je već vraćena"));

        mockMvc.perform(put("/loans/1/return"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Pozajmica sa ID 1 je već vraćena"));
    }

    @Test
    void createLoanReturns503WhenDependencyIsDown() throws Exception {
        given(loanService.createLoan(any(LoanRequestDto.class)))
                .willThrow(new ServiceUnavailableException("member-service trenutno nije dostupan"));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoanRequestDto(5L, 7L))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("member-service trenutno nije dostupan"));
    }

    @Test
    void returnLoanReturns404WhenMissing() throws Exception {
        given(loanService.returnLoan(99L))
                .willThrow(new ResourceNotFoundException("Pozajmica sa ID 99 nije pronađena"));

        mockMvc.perform(put("/loans/99/return"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/loans/99/return"));
    }

}
