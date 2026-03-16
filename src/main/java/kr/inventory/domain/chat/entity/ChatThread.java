package kr.inventory.domain.chat.entity;

import jakarta.persistence.*;
import kr.inventory.domain.chat.entity.enums.ChatThreadStatus;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_threads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatThread extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long threadId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private UUID storePublicId;

    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatThreadStatus status;

    private OffsetDateTime lastMessageAt;

    public static ChatThread create(User user, String title, UUID storePublicId) {
        ChatThread thread = new ChatThread();
        thread.user = user;
        thread.title = title;
        thread.storePublicId = storePublicId;
        thread.status = ChatThreadStatus.ACTIVE;
        return thread;
    }

    public void touchLastMessageAt(OffsetDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
}