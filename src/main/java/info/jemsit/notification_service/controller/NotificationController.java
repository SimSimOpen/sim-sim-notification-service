package info.jemsit.notification_service.controller;

import info.jemsit.notification_service.service.NotificationService;
import info.jemsit.notification_service.service.SmsRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents(@RequestHeader("Authorization") String token) {

        log.info("New SSE connection established");

        return Flux.concat(
                        // Send initial connection message
                        Flux.just(ServerSentEvent.<String>builder()
                                .event("connected")
                                .data("Connection established")
                                .build()),

                        // Stream notifications
                        Flux.merge(
                                notificationService.getNotificationStream(),
                                notificationService.createStreamForUser(token)
                        ).map(notification -> {
                            // Send keepalives as heartbeat events
                            if ("keepalive".equals(notification)) {
                                return ServerSentEvent.<String>builder()
                                        .event("heartbeat")
                                        .data("ping")
                                        .build();
                            }

                            // Send real notifications
                            return ServerSentEvent.<String>builder()
                                    .event("media-notification")
                                    .data(notification)
                                    .build();
                        })
                )
                .doOnCancel(() -> log.info("SSE connection cancelled by client"))
                .doOnComplete(() -> log.warn("SSE connection completed"))
                .doOnError(error -> log.error("SSE connection error", error));
    }


    @PostMapping("send-otp")
    public Mono<?> sendOtp(@RequestBody SmsRequestDTO request) {
        return notificationService.sendOTP(request)
                .doOnSuccess(result -> log.info("OTP sent successfully for token: {}", request))
                .doOnError(error -> log.error("Failed to send OTP for token: {}. Error: {}", request, error.getMessage()));
    }

    @GetMapping("verify-otp")
    public Mono<Boolean> verifyOtp(@RequestParam String phoneNumber, @RequestParam String otp) {
        return notificationService.verifyOTP(phoneNumber, otp)
                .doOnSuccess(result -> log.info("OTP verification result for phone {}: {}", phoneNumber, result))
                .doOnError(error -> log.error("OTP verification failed for phone {}. Error: {}", phoneNumber, error.getMessage()));
    }

}
