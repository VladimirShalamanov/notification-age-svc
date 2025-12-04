package app.service;

import app.exception.NotificationPreferenceDisabledException;
import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationUTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationPreferenceService preferenceService;
    @Mock
    private MailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private NotificationPreference prefEnabled;
    private NotificationPreference prefDisabled;
    private NotificationRequest request;

    @BeforeEach
    void setup() {

        userId = UUID.randomUUID();

        prefEnabled = NotificationPreference.builder()
                .userId(userId)
                .enabled(true)
                .contactInfo("test@example.com")
                .build();

        prefDisabled = NotificationPreference.builder()
                .userId(userId)
                .enabled(false)
                .contactInfo("test@example.com")
                .build();

        request = NotificationRequest.builder()
                .userId(userId)
                .subject("Test Subject")
                .body("Test Body")
                .build();
    }

    @Test
    void send_whenPreferenceIsOff_throwsException() {

        when(preferenceService.getByUserId(userId)).thenReturn(prefDisabled);

        assertThrows(IllegalStateException.class, () -> notificationService.send(request));

        verifyNoInteractions(mailSender);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void send_whenMailSucceeds_setsStatusSucceeded() {

        when(preferenceService.getByUserId(userId)).thenReturn(prefEnabled);

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.send(request);

        assertEquals(NotificationStatus.SUCCEEDED, result.getStatus());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(1)).save(any());
    }

    @Test
    void send_whenMailFails_setsStatusFailed() {

        when(preferenceService.getByUserId(userId)).thenReturn(prefEnabled);

        doThrow(new RuntimeException("Mail error")).when(mailSender).send(any(SimpleMailMessage.class));

        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.send(request);

        assertEquals(NotificationStatus.FAILED, result.getStatus());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(notificationRepository).save(any());
    }

    @Test
    void getHistory_filtersOutDeleted() {

        Notification n1 = Notification.builder().deleted(false).build();
        Notification n2 = Notification.builder().deleted(true).build();

        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(n1, n2));

        List<Notification> result = notificationService.getHistory(userId);

        assertEquals(1, result.size());
        assertSame(n1, result.get(0));
    }

    @Test
    void deleteAll_marksAllAsDeleted() {

        Notification n1 = Notification.builder().deleted(false).build();
        Notification n2 = Notification.builder().deleted(false).build();

        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(n1, n2));

        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.deleteAll(userId);

        assertTrue(n1.isDeleted());
        assertTrue(n2.isDeleted());

        verify(notificationRepository, times(2)).save(any());
    }

    @Test
    void retryFailed_whenPreferenceIsTurnedOff_thenThrowsException() {

        when(preferenceService.getByUserId(userId)).thenReturn(prefDisabled);

        assertThrows(NotificationPreferenceDisabledException.class, () -> notificationService.retryFailed(userId));

        verifyNoInteractions(mailSender);
    }

    @Test
    void retryFailed_whenPreferenceOn_andTwoFailedEmails_retryExactlyTwo() {

        when(preferenceService.getByUserId(userId)).thenReturn(prefEnabled);

        Notification failed1 = Notification.builder()
                .status(NotificationStatus.FAILED)
                .deleted(false)
                .build();
        Notification failed2 = Notification.builder()
                .status(NotificationStatus.FAILED)
                .deleted(false)
                .build();
        Notification deletedFailed = Notification.builder()
                .status(NotificationStatus.FAILED)
                .deleted(true)
                .build();

        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(failed1, failed2, deletedFailed));

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.retryFailed(userId);

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));

        assertEquals(NotificationStatus.SUCCEEDED, failed1.getStatus());
        assertEquals(NotificationStatus.SUCCEEDED, failed2.getStatus());
        assertEquals(NotificationStatus.FAILED, deletedFailed.getStatus());
    }

    @Test
    void getPreferenceByUserId_delegatesCorrectly() {

        when(preferenceService.getByUserId(userId)).thenReturn(prefEnabled);

        NotificationPreference result = notificationService.getPreferenceByUserId(userId);

        assertSame(prefEnabled, result);
        verify(preferenceService).getByUserId(userId);
    }
}