package kr.inventory.domain.notification.repository;

import kr.inventory.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface NotificationRepositoryCustom {

    Page<Notification> findActivePageByUserId(Long userId, Pageable pageable);

    long countUnreadActiveByUserId(Long userId);

    Optional<Notification> findActiveByNotificationIdAndUserId(Long notificationId, Long userId);

    int markAllRead(Long userId, OffsetDateTime now);
}