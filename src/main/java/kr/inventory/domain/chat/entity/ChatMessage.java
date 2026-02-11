package kr.inventory.domain.chat.entity;

import jakarta.persistence.*;
import kr.inventory.domain.chat.entity.enums.ChatMessageRole;
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

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    public static ChatMessage create(ChatThread thread, ChatMessageRole role, String content) {
        ChatMessage message = new ChatMessage();
        message.thread = thread;
        message.role = role;
        message.content = content;
        return message;
    }
}
