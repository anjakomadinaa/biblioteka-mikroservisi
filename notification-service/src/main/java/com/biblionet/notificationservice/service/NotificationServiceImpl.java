package com.biblionet.notificationservice.service;

import com.biblionet.notificationservice.dto.LoanEvent;
import com.biblionet.notificationservice.dto.NotificationResponseDto;
import com.biblionet.notificationservice.entity.Notification;
import com.biblionet.notificationservice.entity.NotificationType;
import com.biblionet.notificationservice.exception.ResourceNotFoundException;
import com.biblionet.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public NotificationResponseDto createFromEvent(LoanEvent event) {
        NotificationType type = NotificationType.fromEventType(event.getEventType());
        Notification notification = new Notification(buildMessage(type, event), type, event.getLoanId());
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    public List<NotificationResponseDto> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public NotificationResponseDto getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Obaveštenje sa ID " + id + " nije pronađeno"));
        return toResponse(notification);
    }

    private String buildMessage(NotificationType type, LoanEvent event) {
        return switch (type) {
            case LOAN_CREATED -> "Pozajmica #" + event.getLoanId() + " je kreirana za člana #" + event.getMemberId() + ".";
            case LOAN_RETURNED -> "Knjiga iz pozajmice #" + event.getLoanId() + " je vraćena.";
        };
    }

    private NotificationResponseDto toResponse(Notification notification) {
        return new NotificationResponseDto(
                notification.getId(),
                notification.getMessage(),
                notification.getType(),
                notification.getRelatedLoanId(),
                notification.getCreatedAt()
        );
    }

}
