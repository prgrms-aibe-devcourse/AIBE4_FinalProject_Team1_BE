package kr.inventory.domain.notification.service.publish;

import com.fasterxml.jackson.databind.JsonNode;
import kr.inventory.domain.notification.entity.Notification;
import kr.inventory.domain.notification.exception.NotificationErrorCode;
import kr.inventory.domain.notification.exception.NotificationException;
import kr.inventory.domain.notification.repository.NotificationRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPublishService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Long publish(NotificationPublishCommand command) {
        User user = userRepository.getReferenceById(command.userId());

        JsonNode storePublicIdNode = command.metadata().get("storePublicId");
        if (storePublicIdNode == null || !storePublicIdNode.isTextual()) {
            throw new NotificationException(NotificationErrorCode.STORE_PUBLIC_ID_NOT_FOUND_IN_METADATA);
        }

        UUID storePublicId = UUID.fromString(storePublicIdNode.asText());
        Store store = storeRepository.findByStorePublicId(storePublicId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.STORE_NOT_FOUND));

        Notification saved = notificationRepository.save(
                Notification.create(
                        user,
                        store,
                        command.type(),
                        command.title(),
                        command.message(),
                        command.deepLink(),
                        command.metadata()
                )
        );

        applicationEventPublisher.publishEvent(
                NotificationPublishedEvent.from(command.userId(), saved.getNotificationId())
        );

        return saved.getNotificationId();
    }
}