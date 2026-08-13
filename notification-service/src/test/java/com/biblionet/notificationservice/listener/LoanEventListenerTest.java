package com.biblionet.notificationservice.listener;

import com.biblionet.notificationservice.dto.LoanEvent;
import com.biblionet.notificationservice.entity.Notification;
import com.biblionet.notificationservice.entity.NotificationType;
import com.biblionet.notificationservice.repository.NotificationRepository;
import com.biblionet.notificationservice.service.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Listener se testira zajedno sa pravim servisom, a mokuje se samo repozitorijum -
 * tako se pokriva i tekst poruke koji je stvarna logika ovog servisa.
 */
@ExtendWith(MockitoExtension.class)
class LoanEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;

    private LoanEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new LoanEventListener(new NotificationServiceImpl(notificationRepository));
    }

    @Test
    void loanCreatedEventIsStoredAsNotification() {
        stubSaveEchoesEntity();

        listener.onLoanEvent(new LoanEvent("LOAN_CREATED", 1L, 5L, 7L, LocalDateTime.now()));

        Notification saved = captureSaved();
        assertThat(saved.getType()).isEqualTo(NotificationType.LOAN_CREATED);
        assertThat(saved.getRelatedLoanId()).isEqualTo(1L);
        assertThat(saved.getMessage()).isEqualTo("Pozajmica #1 je kreirana za člana #5.");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void loanReturnedEventIsStoredAsNotification() {
        stubSaveEchoesEntity();

        listener.onLoanEvent(new LoanEvent("LOAN_RETURNED", 2L, 5L, 7L, LocalDateTime.now()));

        Notification saved = captureSaved();
        assertThat(saved.getType()).isEqualTo(NotificationType.LOAN_RETURNED);
        assertThat(saved.getRelatedLoanId()).isEqualTo(2L);
        assertThat(saved.getMessage()).isEqualTo("Knjiga iz pozajmice #2 je vraćena.");
    }

    @Test
    void unknownEventTypeIsDiscardedWithoutFailing() {
        // ne sme da baci - inace bi se poruka vracala u red u beskonacnoj petlji
        listener.onLoanEvent(new LoanEvent("LOAN_EXPLODED", 3L, 5L, 7L, LocalDateTime.now()));

        verify(notificationRepository, never()).save(any());
    }

    private void stubSaveEchoesEntity() {
        given(notificationRepository.save(any(Notification.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    private Notification captureSaved() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        return captor.getValue();
    }

}
