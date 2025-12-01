package app.web;

import app.exception.NotificationPreferenceDisabledException;
import app.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(NotificationPreferenceDisabledException.class)
    public ResponseEntity<ErrorResponse> handleNotificationPreferenceDisabledException(NotificationPreferenceDisabledException e) {

        ErrorResponse dto = new ErrorResponse(LocalDateTime.now(), e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(dto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        ErrorResponse dto = new ErrorResponse(LocalDateTime.now(), e.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(dto);
    }
}