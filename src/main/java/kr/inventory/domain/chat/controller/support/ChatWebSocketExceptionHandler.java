package kr.inventory.domain.chat.controller.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import kr.inventory.domain.chat.constant.ChatConstants;
import kr.inventory.domain.chat.controller.dto.request.ChatSendMessageRequest;
import kr.inventory.domain.chat.controller.dto.response.ChatRealtimeEventResponse;
import kr.inventory.domain.chat.controller.dto.response.ChatRealtimeEventType;
import kr.inventory.domain.chat.entity.enums.ChatMessageStatus;
import kr.inventory.domain.chat.exception.ChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class ChatWebSocketExceptionHandler {

    private final ObjectMapper objectMapper;

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = ChatConstants.USER_QUEUE_DESTINATION, broadcast = false)
    public ChatRealtimeEventResponse handleException(Exception exception, Message<?> message) {
        ChatSendMessageRequest request = extractRequest(message);
        String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());

        log.warn(
                "Handled chat WebSocket exception. sessionId={}, destination={}, threadId={}, clientMessageId={}, reason={}",
                sessionId,
                destination,
                request != null ? request.threadId() : null,
                request != null ? request.clientMessageId() : null,
                exception.getMessage(),
                exception
        );

        return new ChatRealtimeEventResponse(
                ChatRealtimeEventType.CHAT_FAILED,
                request != null ? request.threadId() : null,
                null,
                request != null ? request.clientMessageId() : null,
                ChatMessageStatus.FAILED,
                null,
                resolveErrorMessage(exception),
                OffsetDateTime.now()
        );
    }

    private ChatSendMessageRequest extractRequest(Message<?> message) {
        if (message == null) {
            return null;
        }

        Object payload = message.getPayload();
        if (payload instanceof ChatSendMessageRequest request) {
            return request;
        }

        try {
            if (payload instanceof byte[] bytes) {
                return objectMapper.readValue(bytes, ChatSendMessageRequest.class);
            }

            if (payload instanceof String text) {
                return objectMapper.readValue(text, ChatSendMessageRequest.class);
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private String resolveErrorMessage(Exception exception) {
        if (exception instanceof ChatException chatException) {
            return chatException.getMessage();
        }

        if (exception instanceof MethodArgumentNotValidException validationException) {
            FieldError fieldError = validationException.getBindingResult().getFieldError();
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                return fieldError.getDefaultMessage();
            }
        }

        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "채팅 요청을 처리하지 못했습니다.";
        }

        return message;
    }
}
