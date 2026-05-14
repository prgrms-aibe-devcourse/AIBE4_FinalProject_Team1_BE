package kr.inventory.domain.chat.entity;

import jakarta.persistence.*;
import kr.inventory.domain.chat.entity.enums.ChatMessageRole;
import kr.inventory.domain.chat.entity.enums.ChatMessageStatus;
import kr.inventory.domain.common.CreatedAtEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private ChatThread thread;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageStatus status;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(length = 100)
    private String clientMessageId;

    private Long replyToMessageId;

    @Column(length = 100)
    private String model;

    @Column(columnDefinition = "text")
    private String errorMessage;

    public static ChatMessage createUserMessage(ChatThread thread, String content, String clientMessageId) {
        ChatMessage message = new ChatMessage();
        message.thread = thread;
        message.role = ChatMessageRole.USER;
        message.status = ChatMessageStatus.QUEUED;
        message.content = content;
        message.clientMessageId = clientMessageId;
        return message;
    }

    public static ChatMessage createAssistantMessage(
            ChatThread thread,
            String content,
            Long replyToMessageId,
            String model
    ) {
        ChatMessage message = new ChatMessage();
        message.thread = thread;
        message.role = ChatMessageRole.ASSISTANT;
        message.status = ChatMessageStatus.COMPLETED;
        message.content = content;
        message.replyToMessageId = replyToMessageId;
        message.model = model;
        return message;
    }

    public void markQueued() {
        this.status = ChatMessageStatus.QUEUED;
        this.errorMessage = null;
    }

    public void markProcessing() {
        this.status = ChatMessageStatus.PROCESSING;
        this.errorMessage = null;
    }

    public void markCompleted() {
        this.status = ChatMessageStatus.COMPLETED;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = ChatMessageStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markInterrupted(String reason) {
        this.status = ChatMessageStatus.INTERRUPTED;
        this.errorMessage = reason;
    }
}