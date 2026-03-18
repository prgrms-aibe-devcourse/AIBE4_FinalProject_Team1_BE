package kr.inventory.domain.notification.repository.impl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import kr.inventory.domain.notification.entity.Notification;
import kr.inventory.domain.notification.entity.QNotification;
import kr.inventory.domain.notification.repository.NotificationRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Page<Notification> findActivePageByUserId(Long userId, UUID storePublicId, Pageable pageable) {
        QNotification notification = QNotification.notification;

        List<Notification> content = queryFactory
                .selectFrom(notification)
                .where(
                        notification.user.userId.eq(userId),
                        notification.deleted.isFalse(),
                        notification.storePublicId.eq(storePublicId)
                )
                .orderBy(notification.createdAt.desc(), notification.notificationId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(notification.count())
                .from(notification)
                .where(
                        notification.user.userId.eq(userId),
                        notification.deleted.isFalse(),
                        notification.storePublicId.eq(storePublicId)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0L : total);
    }

    @Override
    public long countUnreadActiveByUserId(Long userId, UUID storePublicId) {
        QNotification notification = QNotification.notification;

        Long count = queryFactory
                .select(notification.count())
                .from(notification)
                .where(
                        notification.user.userId.eq(userId),
                        notification.deleted.isFalse(),
                        notification.read.isFalse(),
                        notification.storePublicId.eq(storePublicId)
                )
                .fetchOne();

        return count == null ? 0L : count;
    }

    @Override
    public Optional<Notification> findActiveByNotificationIdAndUserId(Long notificationId, Long userId) {
        QNotification notification = QNotification.notification;

        Notification result = queryFactory
                .selectFrom(notification)
                .where(
                        notification.notificationId.eq(notificationId),
                        notification.user.userId.eq(userId),
                        notification.deleted.isFalse()
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public int markAllRead(Long userId, UUID storePublicId, OffsetDateTime now) {
        QNotification notification = QNotification.notification;

        long updated = queryFactory
                .update(notification)
                .set(notification.read, true)
                .set(notification.readAt, now)
                .where(
                        notification.user.userId.eq(userId),
                        notification.deleted.isFalse(),
                        notification.read.isFalse(),
                        notification.storePublicId.eq(storePublicId)
                )
                .execute();

        entityManager.clear();
        return (int) updated;
    }
}