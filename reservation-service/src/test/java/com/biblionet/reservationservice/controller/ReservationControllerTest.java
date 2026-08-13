package com.biblionet.reservationservice.controller;

import com.biblionet.reservationservice.dto.ReservationRequestDto;
import com.biblionet.reservationservice.dto.ReservationResponseDto;
import com.biblionet.reservationservice.entity.ReservationStatus;
import com.biblionet.reservationservice.exception.DuplicateReservationException;
import com.biblionet.reservationservice.exception.ResourceNotFoundException;
import com.biblionet.reservationservice.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    private static final LocalDateTime REQUEST_DATE = LocalDateTime.of(2026, 8, 13, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void createReservationReturns201WithWaitingStatus() throws Exception {
        given(reservationService.createReservation(any(ReservationRequestDto.class))).willReturn(
                new ReservationResponseDto(1L, 5L, 7L, REQUEST_DATE, ReservationStatus.WAITING));

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservationRequestDto(5L, 7L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.memberId").value(5))
                .andExpect(jsonPath("$.bookId").value(7))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.requestDate").value("2026-08-13T10:00:00"));
    }

    @Test
    void createReservationWithMissingFieldsReturns400() throws Exception {
        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.memberId").exists())
                .andExpect(jsonPath("$.errors.bookId").exists());

        verifyNoInteractions(reservationService);
    }

    @Test
    void createDuplicateReservationReturns409() throws Exception {
        given(reservationService.createReservation(any(ReservationRequestDto.class)))
                .willThrow(new DuplicateReservationException("Član #5 već čeka na knjigu #7"));

        mockMvc.perform(post("/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReservationRequestDto(5L, 7L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Član #5 već čeka na knjigu #7"));
    }

    @Test
    void getAllReservationsReturnsList() throws Exception {
        given(reservationService.getAllReservations()).willReturn(List.of(
                new ReservationResponseDto(1L, 5L, 7L, REQUEST_DATE, ReservationStatus.NOTIFIED),
                new ReservationResponseDto(2L, 6L, 7L, REQUEST_DATE.plusHours(1), ReservationStatus.WAITING)
        ));

        mockMvc.perform(get("/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("NOTIFIED"))
                .andExpect(jsonPath("$[1].status").value("WAITING"));
    }

    @Test
    void getReservationByIdReturnsReservation() throws Exception {
        given(reservationService.getReservationById(1L)).willReturn(
                new ReservationResponseDto(1L, 5L, 7L, REQUEST_DATE, ReservationStatus.WAITING));

        mockMvc.perform(get("/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void getReservationByIdReturns404WhenMissing() throws Exception {
        given(reservationService.getReservationById(99L))
                .willThrow(new ResourceNotFoundException("Rezervacija sa ID 99 nije pronađena"));

        mockMvc.perform(get("/reservations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Rezervacija sa ID 99 nije pronađena"))
                .andExpect(jsonPath("$.path").value("/reservations/99"));
    }

    @Test
    void cancelReservationReturnsCancelledStatus() throws Exception {
        given(reservationService.cancelReservation(1L)).willReturn(
                new ReservationResponseDto(1L, 5L, 7L, REQUEST_DATE, ReservationStatus.CANCELLED));

        mockMvc.perform(delete("/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(reservationService).cancelReservation(1L);
    }

    @Test
    void cancelReservationReturns404WhenMissing() throws Exception {
        given(reservationService.cancelReservation(99L))
                .willThrow(new ResourceNotFoundException("Rezervacija sa ID 99 nije pronađena"));

        mockMvc.perform(delete("/reservations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

}
