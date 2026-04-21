package info.jemsit.notification_service.service;

import info.jemsit.common.dto.message.RabbitMQMessage;
import info.jemsit.common.dto.response.auth.AuthenticationResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationService {
    void handleRabbitMQMessage(RabbitMQMessage event);

    Flux<String> getNotificationStream();

    Flux<String> createStreamForUser(String token);

    Mono<?> sendOTP(SmsRequestDTO request);

    Mono<AuthenticationResponseDTO> verifyOTP(String phoneNumber, String otp);
}
