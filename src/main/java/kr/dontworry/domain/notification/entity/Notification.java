package kr.dontworry.domain.notification.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.persistence.*;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.notification.entity.enums.NotificationType;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
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

    @Column(nullable = false)
    private Boolean isRead;

    private OffsetDateTime readAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode metadata;

    public static Notification create(
            User user,
            NotificationType type,
            String title,
            String message,
            String deepLink
    ) {
        return create(user, type, title, message, deepLink, JsonNodeFactory.instance.objectNode());
    }

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
        notification.isRead = false;
        notification.metadata = (metadata != null) ? metadata : JsonNodeFactory.instance.objectNode();
        return notification;
    }
}