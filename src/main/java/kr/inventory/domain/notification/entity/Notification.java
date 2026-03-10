package kr.inventory.domain.notification.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.notification.entity.enums.NotificationType;
import kr.inventory.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_deleted_created", columnList = "user_id, is_deleted, created_at"),
                @Index(name = "idx_notifications_user_read_deleted", columnList = "user_id, is_read, is_deleted")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false, columnDefinition = "text")
    private String deepLink;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    private OffsetDateTime readAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    private OffsetDateTime deletedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode metadata;

    public static Notification create(
            User user,
            NotificationType type,
            String title,
            String message,
            String deepLink,
            JsonNode metadata
    ) {
        Notification notification = new Notification();
        notification.user = user;
        notification.type = type;
        notification.title = title;
        notification.message = message;
        notification.deepLink = deepLink;
        notification.read = false;
        notification.readAt = null;
        notification.deleted = false;
        notification.deletedAt = null;
        notification.metadata = metadata != null ? metadata : JsonNodeFactory.instance.objectNode();
        return notification;
    }

    public void markRead(OffsetDateTime readAt) {
        if (this.deleted || this.read) {
            return;
        }
        this.read = true;
        this.readAt = readAt;
    }

    public void softDelete(OffsetDateTime deletedAt) {
        if (this.deleted) {
            return;
        }
        this.deleted = true;
        this.deletedAt = deletedAt;
    }
}