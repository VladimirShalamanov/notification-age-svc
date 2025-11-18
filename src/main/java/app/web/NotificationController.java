package app.web;

import app.model.Notification;
import app.service.NotificationService;
import app.web.dto.NotificationRequest;
import app.web.dto.NotificationResponse;
import app.web.mapper.DtoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(@RequestBody NotificationRequest request) {

        Notification notification = notificationService.send(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DtoMapper.from(notification));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getHistory(@RequestParam("userId") UUID userId) {

        List<Notification> notifications = notificationService.getHistory(userId);

        List<NotificationResponse> responses = notifications
                .stream()
                .map(DtoMapper::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    // Only for test - 7.Microservice Architecture
//    @GetMapping("/hello")
//    public ResponseEntity<String> sayHello(@RequestParam String name) {
//
//        return ResponseEntity.ok("Hello, {%s} user!".formatted(name));
//    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll(@RequestParam("userId") UUID userId) {

        notificationService.deleteAll(userId);

        return ResponseEntity.ok(null);
    }

    @PutMapping
    public ResponseEntity<Void> retryFailed(@RequestParam("userId") UUID userId) {

        notificationService.retryFailed(userId);

        return ResponseEntity.ok(null);
    }
}
