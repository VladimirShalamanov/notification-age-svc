package app.web.mapper;

import app.model.Notification;
import app.model.NotificationPreference;
import app.web.dto.NotificationResponse;
import app.web.dto.PreferenceResponse;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class DtoMapper {

    public static PreferenceResponse from(NotificationPreference preference) {

        return PreferenceResponse.builder()
                .type(preference.getType())
                .contactInfo(preference.getContactInfo())
                .notificationEnabled(preference.isEnabled())
                .build();
    }

    public static NotificationResponse from(Notification notification) {

        return NotificationResponse.builder()
                .subject(notification.getSubject())
                .createdOn(LocalDateTime.now())
                .status(notification.getStatus())
                .type(notification.getType())
                .build();
    }
}