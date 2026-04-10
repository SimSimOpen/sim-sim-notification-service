package info.jemsit.notification_service.service.impl;

import info.jemsit.common.dto.message.MediaFromMobileStarted;
import info.jemsit.common.dto.message.MediaUploaded;
import info.jemsit.common.dto.message.RabbitMQMessage;
import info.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import info.jemsit.notification_service.service.NotificationService;
import info.jemsit.notification_service.service.SmsRequestDTO;
import info.jemsit.notification_service.service.SmsService;
import info.jemsit.notification_service.service.SseSinkRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static info.jemsit.common.data.constants.RabbitMQConstants.MEDIA_QUEUE;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SseSinkRegistry sseSinkRegistry;

    private final SmsService smsService;

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private final WebClient webClient;

    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer(1000, false);

    @Override
    @RabbitListener(queues = MEDIA_QUEUE)
    public void handleRabbitMQMessage(RabbitMQMessage event) {
        switch (event) {
            case MediaFromMobileStarted m -> notifyUser(m.getId(), m.getMessageString());
            case MediaUploaded m -> notifyUser(m.getId(), m.getMessageString());
            default -> log.warn("Received unknown message type: {}", event.getClass().getSimpleName());
        }
    }

    public void sendMessageToStream(String message) {
        log.info("Message received from RabbitMQ, {}", message);
        Sinks.EmitResult result = sink.tryEmitNext(message);
        if (result.isFailure()) {
            log.error("Failed to emit message:{} - Result: {} - SINK IS DEAD, NEEDS RESTART",
                    message, result);
        }
    }

    public Flux<String> getNotificationStream() {
        return Flux.merge(
                        sink.asFlux()
                                .doOnNext(msg -> log.info("Emitting message to subscribers: {}", msg)),
                        Flux.interval(Duration.ofSeconds(20))
                                .map(tick -> {
                                    log.info("Sending heartbeat");
                                    return "Keep-Alive";
                                })
                ).doOnSubscribe(s -> log.info("New subscriber to notification stream"))
                .doOnCancel(() -> log.info("Subscriber cancelled from notification stream"))
                .doOnError(error -> log.error("Error in notification stream: {}", error.getMessage()))
                .doOnComplete(() -> log.info("Notification stream completed"));
    }

    @Override
    public Flux<String> createStreamForUser(String token) {
        return getUserIdFromAuthService(token)
                .flatMapMany(userId -> Flux.create(fluxSink -> {
                    sseSinkRegistry.register(userId, fluxSink);
                    fluxSink.onCancel(() -> sseSinkRegistry.unregister(userId, fluxSink));
                    fluxSink.onDispose(() -> sseSinkRegistry.unregister(userId, fluxSink));
                }));
    }

    @Override
    public Mono<?> sendOTP(SmsRequestDTO request) {
        var random = new java.util.Random();
        var otp = String.format("%06d", random.nextInt(1000000));
        String key = request.phoneNumber();
        Duration ttl = Duration.ofMinutes(2);
        return  redisTemplate
                .delete(key)
                .then(redisTemplate.opsForList().rightPush(key, otp))
                .then(redisTemplate.expire(key, ttl))
                .doOnSuccess(ignored->{
                    Mono.fromRunnable(() -> smsService.sendSms(request, otp))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                    null,
                                    err-> log.error("Failed to send OTP SMS: {}", err.getMessage())
                            );
                })

                .then(Mono.just("OTP sent successfully"));
    }

    @Override
    public Mono<Boolean> verifyOTP(String phoneNumber, String otp) {
        log.info("Verifying OTP for phone number:{} ", phoneNumber);
        final String editedPhoneNumber = phoneNumber.trim(); // Ensure the phone number is in the correct format
        return redisTemplate
                .opsForList()
                .leftPop(editedPhoneNumber)
                .map(storedOtp -> {
                    log.info("Stored otp {} and coming otp {} ", storedOtp, otp);
                    if (storedOtp == null) {
                        log.warn("No OTP found for phone number: {}", editedPhoneNumber);
                        return false;
                    }
                    boolean isValid = storedOtp.equals(otp);
                    if (!isValid) {
                        log.warn("Invalid OTP attempt for phone number: {}", editedPhoneNumber);
                    }
                    return isValid;
                })
                .defaultIfEmpty(false); // in case the list is empty
    }

    public void notifyUser(String userId, String message) {
        sseSinkRegistry.sendNotificationToUser(userId, message);
    }

    private Mono<String> getUserIdFromAuthService(String token) {
        return webClient.get()
                .uri("/api/auth/v1/details")
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(UserDetailsResponseDTO.class)
                .map(user -> {
                    log.info("Retrieved user ID {} from auth service", user.id());
                    return user.id().toString();
                })
                .doOnError(error -> log.error("Failed to retrieve user details from auth service: {}", error.getMessage()));
    }
}
