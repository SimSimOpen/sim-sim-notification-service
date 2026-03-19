package info.jemsit.notification_service.service.impl;

import info.jemsit.notification_service.service.SseSinkRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class SseSinkRegistryImpl implements SseSinkRegistry {

    private final ConcurrentHashMap<String, FluxSink<String>> sinks = new ConcurrentHashMap<>();

    @Override
    public void register(String userId, FluxSink<String> sink) {
        FluxSink<String> existingSink = sinks.put(userId, sink);
        if (existingSink != null) {
            existingSink.complete();
        }
        System.out.println( "Sink is " + sinks.keySet());
    }

    @Override
    public void unregister(String userId) {
        sinks.remove(userId);
    }

    @Override
    public void sendNotificationToUser(String userId, String message) {
        log.info("Sending notification to user {}: {}", userId, message);
        FluxSink<String> sink = sinks.get(userId);
        if (sink != null) {
            sink.next(message);
        }
    }

    @Override
    public boolean isConnected(String userId) {
        return sinks.containsKey(userId);
    }
}
