package com.biblionet.notificationservice.service;

import com.biblionet.notificationservice.dto.LoanEvent;
import com.biblionet.notificationservice.dto.NotificationResponseDto;

import java.util.List;

public interface NotificationService {

    NotificationResponseDto createFromEvent(LoanEvent event);

    List<NotificationResponseDto> getAllNotifications();

    NotificationResponseDto getNotificationById(Long id);

}
