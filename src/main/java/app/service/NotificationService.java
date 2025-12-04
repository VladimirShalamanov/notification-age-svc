package app.service;

import app.exception.NotificationPreferenceDisabledException;
import app.model.Notification;
import app.model.NotificationPreference;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.repository.NotificationRepository;
import app.web.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService preferenceService;
    private final MailSender mailSender;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository,
                               NotificationPreferenceService preferenceService,
                               MailSender mailSender) {

        this.notificationRepository = notificationRepository;
        this.preferenceService = preferenceService;
        this.mailSender = mailSender;
    }

    public Notification send(NotificationRequest request) {

        NotificationPreference preference = preferenceService.getByUserId(request.getUserId());

        if (!preference.isEnabled()) {
            throw new IllegalStateException("User with id=[%s] turned off their notifications.".formatted(request.getUserId()));
        }

        Notification notification = Notification.builder()
                .subject(request.getSubject())
                .body(request.getBody())
                .createdOn(LocalDateTime.now())
                .type(NotificationType.EMAIL)
                .userId(request.getUserId())
                .deleted(false)
                .build();

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(preference.getContactInfo());
        mailMessage.setSubject(request.getSubject());
        mailMessage.setText(request.getBody());

        sendMail(mailMessage, notification);

        log.info("---Send notification for [%s].".formatted(preference.getContactInfo()));

        return notificationRepository.save(notification);
    }

    public List<Notification> getHistory(UUID userId) {

        return notificationRepository
                .findByUserId(userId)
                .stream()
                .filter(n -> !n.isDeleted())
                .toList();
    }

    public void deleteAll(UUID userId) {

        List<Notification> notifications = getHistory(userId);

        for (Notification notification : notifications) {

            notification.setDeleted(true);
            notificationRepository.save(notification);
        }

        NotificationPreference preference = preferenceService.getByUserId(userId);
        log.info("---Delete all notifications for [%s].".formatted(preference.getContactInfo()));
    }

    public void retryFailed(UUID userId) {

        NotificationPreference preference = preferenceService.getByUserId(userId);
        if (!preference.isEnabled()) {
            throw new NotificationPreferenceDisabledException("User does turned off their notifications.");
        }

        List<Notification> failedNotifications = getHistory(userId)
                .stream()
                .filter(n -> n.getStatus() == NotificationStatus.FAILED)
                .toList();

        for (Notification failedNotification : failedNotifications) {

            SimpleMailMessage newEmail = new SimpleMailMessage();
            newEmail.setTo(preference.getContactInfo());
            newEmail.setSubject(failedNotification.getSubject());
            newEmail.setText(failedNotification.getBody());

            sendMail(newEmail, failedNotification);

            notificationRepository.save(failedNotification);
        }

        log.info("---Retry failed notifications for [%s].".formatted(preference.getContactInfo()));
    }

    private void sendMail(SimpleMailMessage mailMessage, Notification notification) {

        try {
            mailSender.send(mailMessage);
            notification.setStatus(NotificationStatus.SUCCEEDED);
        } catch (Exception e) {
            log.error("Failed email due to: {}", e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
        }
    }

    public NotificationPreference getPreferenceByUserId(UUID userId) {

        return preferenceService.getByUserId(userId);
    }
}
