package com.biblionet.notificationservice.controller;

import com.biblionet.notificationservice.dto.NotificationResponseDto;
import com.biblionet.notificationservice.entity.NotificationType;
import com.biblionet.notificationservice.exception.ResourceNotFoundException;
import com.biblionet.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 13, 18, 30, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void getAllNotificationsReturnsList() throws Exception {
        given(notificationService.getAllNotifications()).willReturn(List.of(
                new NotificationResponseDto(1L, "Pozajmica #1 je kreirana za člana #5.",
                        NotificationType.LOAN_CREATED, 1L, CREATED_AT),
                new NotificationResponseDto(2L, "Knjiga iz pozajmice #1 je vraćena.",
                        NotificationType.LOAN_RETURNED, 1L, CREATED_AT)
        ));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("LOAN_CREATED"))
                .andExpect(jsonPath("$[0].relatedLoanId").value(1))
                .andExpect(jsonPath("$[1].type").value("LOAN_RETURNED"))
                .andExpect(jsonPath("$[1].message").value("Knjiga iz pozajmice #1 je vraćena."));
    }

    @Test
    void getNotificationByIdReturnsNotification() throws Exception {
        given(notificationService.getNotificationById(1L)).willReturn(
                new NotificationResponseDto(1L, "Pozajmica #1 je kreirana za člana #5.",
                        NotificationType.LOAN_CREATED, 1L, CREATED_AT));

        mockMvc.perform(get("/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.message").value("Pozajmica #1 je kreirana za člana #5."))
                .andExpect(jsonPath("$.type").value("LOAN_CREATED"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-13T18:30:00"));
    }

    @Test
    void getNotificationByIdReturns404WhenMissing() throws Exception {
        given(notificationService.getNotificationById(99L))
                .willThrow(new ResourceNotFoundException("Obaveštenje sa ID 99 nije pronađeno"));

        mockMvc.perform(get("/notifications/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Obaveštenje sa ID 99 nije pronađeno"))
                .andExpect(jsonPath("$.path").value("/notifications/99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

}
