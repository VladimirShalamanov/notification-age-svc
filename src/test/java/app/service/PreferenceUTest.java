package app.service;

import app.model.NotificationPreference;
import app.model.NotificationType;
import app.repository.NotificationPreferenceRepository;
import app.web.dto.PreferenceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PreferenceUTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private NotificationPreferenceService preferenceService;

    private UUID userId;
    private PreferenceRequest request;
    private NotificationPreference existingPreference;

    @BeforeEach
    void setup() {

        userId = UUID.randomUUID();

        request = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("test@example.com")
                .build();

        existingPreference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(false)
                .contactInfo("old@example.com")
                .createdOn(LocalDateTime.now().minusDays(5))
                .updatedOn(LocalDateTime.now().minusDays(5))
                .build();
    }

    @Test
    void testUpsert_UpdateExistingPreference() {

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existingPreference));
        when(preferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference result = preferenceService.upsert(request);

        assertEquals(request.isNotificationEnabled(), result.isEnabled());
        assertEquals(request.getContactInfo(), result.getContactInfo());

        verify(preferenceRepository).save(existingPreference);
    }

    @Test
    void testUpsert_CreateNewPreference() {

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreference result = preferenceService.upsert(request);

        assertEquals(userId, result.getUserId());
        assertEquals(NotificationType.EMAIL, result.getType());
        assertEquals(request.getContactInfo(), result.getContactInfo());
        assertEquals(request.isNotificationEnabled(), result.isEnabled());

        verify(preferenceRepository).save(any());
    }

    @Test
    void testGetByUserId_Success() {

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existingPreference));

        NotificationPreference result = preferenceService.getByUserId(userId);

        assertEquals(existingPreference, result);
    }

    @Test
    void testGetByUserId_NotFound() {

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> preferenceService.getByUserId(userId));
    }
}