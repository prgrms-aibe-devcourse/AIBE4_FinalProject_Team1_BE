package kr.inventory.domain.notification.service.trigger;

import kr.inventory.domain.notification.service.publish.NotificationPublishCommand;
import kr.inventory.domain.notification.service.publish.NotificationPublishService;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.store.service.StoreMemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StockThresholdNotificationTriggerService {

    private final StoreMemberQueryService storeMemberQueryService;
    private final NotificationPublishService notificationPublishService;

    public void notifyStoreMembersBelowThreshold(
            Long storeId,
            Ingredient ingredient,
            BigDecimal currentQuantity
    ) {
        BigDecimal threshold = ingredient.getLowStockThreshold();
        if (threshold == null) {
            return;
        }

        List<Long> memberUserIds = storeMemberQueryService.findActiveMemberUserIds(storeId);

        for (Long userId : memberUserIds) {
            NotificationPublishCommand command =
                    NotificationPublishCommand.stockBelowThreshold(
                            userId,
                            ingredient.getStore().getStorePublicId(),
                            ingredient.getIngredientPublicId(),
                            ingredient.getName(),
                            currentQuantity,
                            threshold
                    );

            notificationPublishService.publish(command);
        }
    }
}