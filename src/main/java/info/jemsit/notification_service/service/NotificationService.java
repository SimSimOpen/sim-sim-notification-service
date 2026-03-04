package info.jemsit.notification_service.service;

import info.jemsit.common.dto.message.RabbitMQMessage;
import reactor.core.publisher.Flux;

public interface NotificationService {
     void handleRabbitMQMessage(RabbitMQMessage  event);

    Flux<String> getNotificationStream();

    Flux<String> createStreamForUser(String userId);
}
