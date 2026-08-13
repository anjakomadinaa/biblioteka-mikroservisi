package com.biblionet.notificationservice.dto;

import com.biblionet.notificationservice.entity.NotificationType;

import java.time.LocalDateTime;

public class NotificationResponseDto {

    private Long id;
    private String message;
    private NotificationType type;
    private Long relatedLoanId;
    private LocalDateTime createdAt;

    public NotificationResponseDto() {
    }

    public NotificationResponseDto(Long id, String message, NotificationType type, Long relatedLoanId,
                                   LocalDateTime createdAt) {
        this.id = id;
        this.message = message;
        this.type = type;
        this.relatedLoanId = relatedLoanId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public Long getRelatedLoanId() {
        return relatedLoanId;
    }

    public void setRelatedLoanId(Long relatedLoanId) {
        this.relatedLoanId = relatedLoanId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
