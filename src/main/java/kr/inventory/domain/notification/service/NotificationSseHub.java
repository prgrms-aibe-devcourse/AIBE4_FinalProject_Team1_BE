package kr.inventory.domain.notification.service;

import static kr.inventory.domain.notification.constant.NotificationConstants.CONNECT_EVENT_NAME;
import static kr.inventory.domain.notification.constant.NotificationConstants.DEFAULT_TIMEOUT;
import static kr.inventory.domain.notification.constant.NotificationConstants.NOTIFICATION_EVENT_NAME;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import kr.inventory.domain.notification.controller.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Component
public class NotificationSseHub {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(exception -> removeEmitter(userId, emitter));

        try {
            emitter.send(
                    SseEmitter.event()
                            .name(CONNECT_EVENT_NAME)
                            .data("connected")
            );
        } catch (IOException | IllegalStateException e) {
            log.debug("Failed to send SSE connect event. userId={}, reason={}", userId, e.getMessage());
            removeEmitter(userId, emitter);
            emitter.complete();
        }

        return emitter;
    }

    public void sendNotification(Long userId, NotificationResponse response) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name(NOTIFICATION_EVENT_NAME)
                                .data(response)
                );
            } catch (IOException | IllegalStateException e) {
                log.debug("Failed to send SSE notification. userId={}, reason={}", userId, e.getMessage());
                removeEmitter(userId, emitter);
                emitter.complete();
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }

        userEmitters.remove(emitter);

        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }
}
