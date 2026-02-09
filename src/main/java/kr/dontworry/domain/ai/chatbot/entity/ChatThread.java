package kr.dontworry.domain.ai.chatbot.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.ai.chatbot.entity.enums.ChatThreadStatus;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

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

    @Column(length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatThreadStatus status;

    private OffsetDateTime lastMessageAt;

    public static ChatThread create(User user, String title) {
        ChatThread thread = new ChatThread();
        thread.user = user;
        thread.title = title;
        thread.status = ChatThreadStatus.ACTIVE;
        return thread;
    }
}
