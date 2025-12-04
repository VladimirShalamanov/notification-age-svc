package app.event;

import app.event.payload.UserRegisteredEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

    @KafkaListener(topics = "user-registered-event.v1", groupId = "notification-age-svc")
    public void consumeEvent(UserRegisteredEvent event) {

        System.out.printf("Processed event for user with id=[%s]".formatted(event.getUserId()));
    }
}