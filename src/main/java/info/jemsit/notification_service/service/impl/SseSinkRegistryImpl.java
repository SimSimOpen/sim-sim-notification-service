package info.jemsit.notification_service.service.impl;

import info.jemsit.notification_service.service.SseSinkRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


@Service
@Slf4j
public class SseSinkRegistryImpl implements SseSinkRegistry {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<FluxSink<String>>> sinks = new ConcurrentHashMap<>();

    @Override
    public void register(String userId, FluxSink<String> sink) {
        sinks.computeIfAbsent(userId, k->new CopyOnWriteArrayList<>()).add(sink);
       log.info("Registered new sink for user {}. Total sinks for user: {}", userId, sinks.get(userId).size());
    }

    @Override
    public void unregister(String userId, FluxSink<String> sink) {
        CopyOnWriteArrayList<FluxSink<String>> userSinks = sinks.get(userId);
        if (userSinks != null) {
            userSinks.remove(sink);
            if(userSinks.isEmpty()) sinks.remove(userId);
        }
        log.info("Unregistered sink for user {}. Remaining sinks for user: {}", userId, userSinks != null ? userSinks.size() : 0);
    }

    @Override
    public void sendNotificationToUser(String userId, String message) {
        log.info("Sending notification to user {}: {}", userId, message);
        CopyOnWriteArrayList<FluxSink<String>> userSinks = sinks.get(userId);
        if (userSinks != null) {
            userSinks.forEach(sink -> sink.next(message));
        }else {
            log.warn("No active sinks found for user {}", userId);
        }
    }

    @Override
    public boolean isConnected(String userId) {
        CopyOnWriteArrayList<FluxSink<String>> userSinks = sinks.get(userId);
        return userSinks != null && !userSinks.isEmpty();
    }
}
