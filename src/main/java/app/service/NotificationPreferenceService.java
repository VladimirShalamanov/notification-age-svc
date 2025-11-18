package app.service;

import app.model.NotificationPreference;
import app.model.NotificationType;
import app.repository.NotificationPreferenceRepository;
import app.web.dto.PreferenceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Autowired
    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    public NotificationPreference upsert(PreferenceRequest request) {

        Optional<NotificationPreference> preferenceOpt = preferenceRepository.findByUserId(request.getUserId());

        if (preferenceOpt.isPresent()) {

            NotificationPreference preference = preferenceOpt.get();
            preference.setEnabled(request.isNotificationEnabled());
            preference.setContactInfo(request.getContactInfo());
            preference.setUpdatedOn(LocalDateTime.now());

            return preferenceRepository.save(preference);
        }

        NotificationPreference preference = NotificationPreference.builder()
                .userId(request.getUserId())
                .type(NotificationType.EMAIL)
                .enabled(request.isNotificationEnabled())
                .contactInfo(request.getContactInfo())
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        return preferenceRepository.save(preference);
    }

    public NotificationPreference getByUserId(UUID userId) {

        return preferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Preference for this user does not exist."));
    }
}
