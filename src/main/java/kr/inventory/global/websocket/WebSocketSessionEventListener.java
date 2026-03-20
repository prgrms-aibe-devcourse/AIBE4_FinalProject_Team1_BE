package kr.inventory.global.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Slf4j
@Component
public class WebSocketSessionEventListener {

    @EventListener
    public void onSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("[WebSocket] CONNECT sessionId={}, user={}, destination={}",
                accessor.getSessionId(),
                accessor.getUser() != null ? accessor.getUser().getName() : null,
                accessor.getDestination());
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("[WebSocket] CONNECTED sessionId={}, user={}",
                accessor.getSessionId(),
                accessor.getUser() != null ? accessor.getUser().getName() : null);
    }

    @EventListener
    public void onSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("[WebSocket] SUBSCRIBE sessionId={}, user={}, destination={}",
                accessor.getSessionId(),
                accessor.getUser() != null ? accessor.getUser().getName() : null,
                accessor.getDestination());
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("[WebSocket] DISCONNECT sessionId={}, user={}, closeStatus={}",
                accessor.getSessionId(),
                accessor.getUser() != null ? accessor.getUser().getName() : null,
                event.getCloseStatus());
    }
}
