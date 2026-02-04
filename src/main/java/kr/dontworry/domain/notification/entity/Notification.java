package kr.dontworry.domain.notification.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.ledger.entity.Ledger;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_id", nullable = false)
    private Ledger ledger;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false, columnDefinition = "text")
    private String deepLink;

    @Column(nullable = false)
    private Boolean isRead;

    private OffsetDateTime readAt;

    public static Notification create(
        User user,
        Ledger ledger,
        String type,
        String title,
        String message,
        String deepLink
    ) {
        Notification notification = new Notification();
        notification.user = user;
        notification.ledger = ledger;
        notification.type = type;
        notification.title = title;
        notification.message = message;
        notification.deepLink = deepLink;
        notification.isRead = false;
        return notification;
    }
}
