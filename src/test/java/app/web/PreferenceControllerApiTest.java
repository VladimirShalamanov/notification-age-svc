package app.web;

import app.model.NotificationPreference;
import app.model.NotificationType;
import app.service.NotificationPreferenceService;
import app.web.dto.PreferenceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PreferenceController.class)
public class PreferenceControllerApiTest {

    @MockitoBean
    private NotificationPreferenceService preferenceService;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private PreferenceRequest request;
    private NotificationPreference preference;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();

        request = PreferenceRequest.builder()
                .userId(userId)
                .notificationEnabled(true)
                .contactInfo("test@example.com")
                .build();

        preference = NotificationPreference.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(NotificationType.EMAIL)
                .enabled(request.isNotificationEnabled())
                .contactInfo(request.getContactInfo())
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
    }

    @Test
    void testUpsertPreference_Success() throws Exception {

        when(preferenceService.upsert(any(PreferenceRequest.class))).thenReturn(preference);

        mockMvc.perform(post("/api/v1/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationEnabled").value(preference.isEnabled()))
                .andExpect(jsonPath("$.contactInfo").value(preference.getContactInfo()))
                .andExpect(jsonPath("$.type").value(preference.getType().toString()));
    }

    @Test
    void testGetPreferenceForUser_Success() throws Exception {

        when(preferenceService.getByUserId(userId)).thenReturn(preference);

        mockMvc.perform(get("/api/v1/preferences")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationEnabled").value(preference.isEnabled()))
                .andExpect(jsonPath("$.contactInfo").value(preference.getContactInfo()))
                .andExpect(jsonPath("$.type").value(preference.getType().toString()));
    }

    @Test
    void testGetPreferenceForUser_NotFound() throws Exception {

        when(preferenceService.getByUserId(userId))
                .thenThrow(new RuntimeException("Preference for this user does not exist."));

        mockMvc.perform(get("/api/v1/preferences")
                        .param("userId", userId.toString()))
                .andExpect(status().isInternalServerError());
    }
}