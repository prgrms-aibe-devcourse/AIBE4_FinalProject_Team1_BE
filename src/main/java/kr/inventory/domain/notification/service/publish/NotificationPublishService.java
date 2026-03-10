package kr.inventory.domain.notification.service.publish;

import kr.inventory.domain.notification.entity.Notification;
import kr.inventory.domain.notification.repository.NotificationRepository;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPublishService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Long publish(NotificationPublishCommand command) {
        User user = userRepository.getReferenceById(command.userId());

        Notification saved = notificationRepository.save(
                Notification.create(
                        user,
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