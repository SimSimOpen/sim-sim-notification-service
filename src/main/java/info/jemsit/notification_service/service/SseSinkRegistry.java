package info.jemsit.notification_service.service;

import reactor.core.publisher.FluxSink;

public interface SseSinkRegistry {
    void register(String userId, FluxSink<String> sink);

    void unregister(String userId, FluxSink<String> sink);

    void sendNotificationToUser(String userId, String message);

    boolean isConnected(String userId);
}
