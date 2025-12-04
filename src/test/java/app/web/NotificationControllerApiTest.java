package app.web;

import app.model.Notification;
import app.model.NotificationStatus;
import app.model.NotificationType;
import app.service.NotificationService;
import app.web.dto.NotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
public class NotificationControllerApiTest {

    @MockitoBean
    private NotificationService notificationService;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private NotificationRequest request;
    private Notification notification;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();

        request = NotificationRequest.builder()
                .userId(userId)
                .subject("Test Subject")
                .body("Test Body")
                .build();

        notification = Notification.builder()
                .id(UUID.randomUUID())
                .subject(request.getSubject())
                .body(request.getBody())
                .createdOn(LocalDateTime.now())
                .status(NotificationStatus.SUCCEEDED)
                .type(NotificationType.EMAIL)
                .userId(userId)
                .deleted(false)
                .build();
    }

    @Test
    void testSendNotification_Success() throws Exception {

        when(notificationService.send(any(NotificationRequest.class))).thenReturn(notification);

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value(notification.getSubject()))
                .andExpect(jsonPath("$.status").value(notification.getStatus().toString()))
                .andExpect(jsonPath("$.type").value(notification.getType().toString()));
    }

    @Test
    void testGetHistory_Success() throws Exception {

        List<Notification> notifications = List.of(notification);

        when(notificationService.getHistory(userId)).thenReturn(notifications);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value(notification.getSubject()))
                .andExpect(jsonPath("$[0].status").value(notification.getStatus().toString()));
    }

    @Test
    void testDeleteAll_Success() throws Exception {

        doNothing().when(notificationService).deleteAll(userId);

        mockMvc.perform(delete("/api/v1/notifications")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testRetryFailed_Success() throws Exception {

        doNothing().when(notificationService).retryFailed(userId);

        mockMvc.perform(put("/api/v1/notifications")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testSendNotification_ServiceThrowsException() throws Exception {

        when(notificationService.send(any(NotificationRequest.class)))
                .thenThrow(new IllegalStateException("User notifications disabled"));

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}