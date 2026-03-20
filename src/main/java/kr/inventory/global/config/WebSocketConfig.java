package kr.inventory.global.config;

import kr.inventory.global.auth.interceptor.JwtHandshakeInterceptor;
import kr.inventory.global.auth.interceptor.StompAuthChannelInterceptor;
import kr.inventory.global.constant.WebSocketConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final CorsProperties corsProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOriginPatterns = corsProperties.getAllowedOrigins().toArray(String[]::new);

        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOriginPatterns);

        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOriginPatterns)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
        registration.taskExecutor()
                .corePoolSize(WebSocketConstants.INBOUND_CORE_POOL_SIZE)
                .maxPoolSize(WebSocketConstants.INBOUND_MAX_POOL_SIZE)
                .queueCapacity(WebSocketConstants.CHANNEL_QUEUE_CAPACITY);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(WebSocketConstants.OUTBOUND_CORE_POOL_SIZE)
                .maxPoolSize(WebSocketConstants.OUTBOUND_MAX_POOL_SIZE)
                .queueCapacity(WebSocketConstants.CHANNEL_QUEUE_CAPACITY);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(WebSocketConstants.SIMPLE_BROKER_HEARTBEAT)
                .setTaskScheduler(webSocketBrokerTaskScheduler());
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(WebSocketConstants.MESSAGE_SIZE_LIMIT)
                .setSendBufferSizeLimit(WebSocketConstants.SEND_BUFFER_SIZE_LIMIT)
                .setSendTimeLimit(WebSocketConstants.SEND_TIME_LIMIT_MS)
                .setTimeToFirstMessage(WebSocketConstants.TIME_TO_FIRST_MESSAGE_MS);
    }

    @Bean
    public ThreadPoolTaskScheduler webSocketBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("ws-broker-heartbeat-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}