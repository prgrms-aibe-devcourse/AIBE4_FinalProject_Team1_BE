package kr.inventory.domain.notification.service;

import kr.inventory.domain.notification.constant.NotificationConstants;
import kr.inventory.domain.notification.controller.dto.response.NotificationResponse;
import kr.inventory.domain.notification.entity.Notification;
import kr.inventory.domain.notification.exception.NotificationErrorCode;
import kr.inventory.domain.notification.exception.NotificationException;
import kr.inventory.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseHub notificationSseHub;
    private final Clock clock;

    public SseEmitter connectStream(Long userId) {
        return notificationSseHub.connect(userId);
    }

    public void sendRealtime(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findActiveByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notificationSseHub.sendNotification(
                userId,
                NotificationResponse.from(notification)
        );
    }

    public Page<NotificationResponse> list(Long userId, Pageable pageable) {
        if (pageable.getPageNumber() < NotificationConstants.MIN_PAGE
                || pageable.getPageSize() < NotificationConstants.MIN_SIZE
                || pageable.getPageSize() > NotificationConstants.MAX_SIZE) {
            throw new NotificationException(NotificationErrorCode.INVALID_REQUEST);
        }

        return notificationRepository.findActivePageByUserId(userId, pageable)
                .map(NotificationResponse::from);
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countUnreadActiveByUserId(userId);
    }

    @Transactional
    public void readOne(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findActiveByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markRead(OffsetDateTime.now(clock));
    }

    @Transactional
    public int readAll(Long userId) {
        return notificationRepository.markAllRead(userId, OffsetDateTime.now(clock));
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findActiveByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        notification.softDelete(OffsetDateTime.now(clock));
    }
}