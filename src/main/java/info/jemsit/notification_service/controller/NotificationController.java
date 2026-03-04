package info.jemsit.notification_service.controller;

import info.jemsit.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamEvents() {

        log.info("New SSE connection established");

        return Flux.concat(
                        // Send initial connection message
                        Flux.just(ServerSentEvent.<String>builder()
                                .event("connected")
                                .data("Connection established")
                                .build()),

                        // Stream notifications
                        notificationService.getNotificationStream()
                                .map(notification -> {
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
}
