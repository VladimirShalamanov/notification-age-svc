package app.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PreferenceRequest {

    private UUID userId;

    private boolean notificationEnabled;

    private String contactInfo;
}