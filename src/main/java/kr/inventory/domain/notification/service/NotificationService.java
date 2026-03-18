package kr.inventory.domain.notification.service;

import kr.inventory.domain.notification.constant.NotificationConstants;
import kr.inventory.domain.notification.controller.dto.response.NotificationResponse;
import kr.inventory.domain.notification.entity.Notification;
import kr.inventory.domain.notification.exception.NotificationErrorCode;
import kr.inventory.domain.notification.exception.NotificationException;
import kr.inventory.domain.notification.repository.NotificationRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSseHub notificationSseHub;
    private final StoreAccessValidator storeAccessValidator;
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

    public Page<NotificationResponse> list(Long userId, UUID storePublicId, Pageable pageable) {
        storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        if (pageable.getPageNumber() < NotificationConstants.MIN_PAGE
                || pageable.getPageSize() < NotificationConstants.MIN_SIZE
                || pageable.getPageSize() > NotificationConstants.MAX_SIZE) {
            throw new NotificationException(NotificationErrorCode.INVALID_REQUEST);
        }

        return notificationRepository.findActivePageByUserId(userId, storePublicId, pageable)
                .map(NotificationResponse::from);
    }

    public long unreadCount(Long userId, UUID storePublicId) {
        storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        return notificationRepository.countUnreadActiveByUserId(userId, storePublicId);
    }

    @Transactional
    public void readOne(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findActiveByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markRead(OffsetDateTime.now(clock));
    }

    @Transactional
    public int readAll(Long userId, UUID storePublicId) {
        storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        return notificationRepository.markAllRead(userId, storePublicId, OffsetDateTime.now(clock));
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findActiveByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        notification.softDelete(OffsetDateTime.now(clock));
    }
}