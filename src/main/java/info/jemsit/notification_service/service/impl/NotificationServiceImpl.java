package info.jemsit.notification_service.service.impl;

import info.jemsit.common.dto.message.MediaFromMobileStarted;
import info.jemsit.common.dto.message.MediaUploaded;
import info.jemsit.common.dto.message.RabbitMQMessage;
import info.jemsit.notification_service.service.NotificationService;
import info.jemsit.notification_service.service.SseSinkRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

import static info.jemsit.common.data.constants.RabbitMQConstants.MEDIA_QUEUE;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SseSinkRegistry sseSinkRegistry;

    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer(1000, false);

    @Override
    @RabbitListener(queues = MEDIA_QUEUE)
    public   void handleRabbitMQMessage(RabbitMQMessage event) {
       switch (event) {
           case MediaFromMobileStarted m -> sendMessageToStream(m.getMessageString());
           case MediaUploaded m -> sendMessageToStream(m.getMessageString());
           default -> log.warn("Received unknown message type: {}", event.getClass().getSimpleName());
       }
    }

    public void sendMessageToStream(String message) {
        log.info("Message received from RabbitMQ, {}", message);
        Sinks.EmitResult result = sink.tryEmitNext(message);
        if(result.isFailure()) {
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
        ).doOnSubscribe(s-> log.info("New subscriber to notification stream"))
         .doOnCancel(() -> log.info("Subscriber cancelled from notification stream"))
                .doOnError(error-> log.error("Error in notification stream: {}", error.getMessage()))
                .doOnComplete(()-> log.info("Notification stream completed"));
    }

    @Override
    public Flux<String> createStreamForUser(String userId) {
        return Flux.create(sink -> {
            sseSinkRegistry.register(userId, sink);
            sink.onCancel(() -> sseSinkRegistry.unregister(userId));
        });
    }

//    @RabbitListener(queues = MEDIA_QUEUE)
//    public void notifyUser(String userId, String message) {
//        sseSinkRegistry.sendNotificationToUser(userId, message);
//    }
}
