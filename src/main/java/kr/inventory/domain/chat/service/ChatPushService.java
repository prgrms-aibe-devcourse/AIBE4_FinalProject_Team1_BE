package kr.inventory.domain.chat.service;

import java.time.OffsetDateTime;
import kr.inventory.domain.chat.constant.ChatConstants;
import kr.inventory.domain.chat.controller.dto.response.ChatRealtimeEventResponse;
import kr.inventory.domain.chat.controller.dto.response.ChatRealtimeEventType;
import kr.inventory.domain.chat.entity.enums.ChatMessageStatus;
import kr.inventory.domain.chat.service.command.AcceptedUserMessageResult;
import kr.inventory.domain.chat.service.command.CompletedChatResult;
import kr.inventory.domain.chat.service.command.FailedChatResult;
import kr.inventory.domain.chat.service.command.InterruptedChatResult;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatPushService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendAccepted(AcceptedUserMessageResult result) {
        ChatRealtimeEventResponse payload = new ChatRealtimeEventResponse(
                ChatRealtimeEventType.USER_MESSAGE_ACCEPTED,
                result.requestMessage().threadId(),
                result.requestMessage().messageId(),
                result.requestMessage().clientMessageId(),
                result.requestMessage().status(),
                result.requestMessage(),
                null,
                OffsetDateTime.now()
        );

        messagingTemplate.convertAndSendToUser(String.valueOf(result.userId()), ChatConstants.USER_QUEUE_DESTINATION, payload);
    }

    public void sendProcessing(Long userId, Long threadId, Long requestMessageId, String clientMessageId) {
        ChatRealtimeEventResponse payload = new ChatRealtimeEventResponse(
                ChatRealtimeEventType.CHAT_PROCESSING,
                threadId,
                requestMessageId,
                clientMessageId,
                ChatMessageStatus.PROCESSING,
                null,
                null,
                OffsetDateTime.now()
        );

        messagingTemplate.convertAndSendToUser(String.valueOf(userId), ChatConstants.USER_QUEUE_DESTINATION, payload);
    }

    public void sendCompleted(CompletedChatResult result) {
        ChatRealtimeEventResponse payload = new ChatRealtimeEventResponse(
                ChatRealtimeEventType.CHAT_RESPONSE_CREATED,
                result.assistantMessage().threadId(),
                result.requestMessageId(),
                null,
                ChatMessageStatus.COMPLETED,
                result.assistantMessage(),
                null,
                OffsetDateTime.now()
        );

        messagingTemplate.convertAndSendToUser(String.valueOf(result.userId()), ChatConstants.USER_QUEUE_DESTINATION, payload);
    }

    public void sendFailed(FailedChatResult result) {
        ChatRealtimeEventResponse payload = new ChatRealtimeEventResponse(
                ChatRealtimeEventType.CHAT_FAILED,
                result.threadId(),
                result.requestMessageId(),
                result.clientMessageId(),
                ChatMessageStatus.FAILED,
                null,
                result.errorMessage(),
                OffsetDateTime.now()
        );

        messagingTemplate.convertAndSendToUser(String.valueOf(result.userId()), ChatConstants.USER_QUEUE_DESTINATION, payload);
    }

    public void sendInterrupted(InterruptedChatResult result) {
        ChatRealtimeEventResponse payload = new ChatRealtimeEventResponse(
                ChatRealtimeEventType.CHAT_INTERRUPTED,
                result.threadId(),
                result.requestMessageId(),
                result.clientMessageId(),
                ChatMessageStatus.INTERRUPTED,
                null,
                result.reason(),
                OffsetDateTime.now()
        );

        messagingTemplate.convertAndSendToUser(String.valueOf(result.userId()), ChatConstants.USER_QUEUE_DESTINATION, payload);
    }
}
