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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StockShortageNotificationTriggerService {

    private final StoreMemberQueryService storeMemberQueryService;
    private final NotificationPublishService notificationPublishService;

    public void notifyStoreMembersStockShortage(
            Long storeId,
            Map<Long, BigDecimal> usageMap,
            Map<Long, BigDecimal> shortageMap,
            Map<Long, Ingredient> ingredientMap
    ) {
        if (shortageMap == null || shortageMap.isEmpty()) {
            return;
        }

        List<Long> memberUserIds = storeMemberQueryService.findActiveMemberUserIds(storeId);

        for (Map.Entry<Long, BigDecimal> entry : shortageMap.entrySet()) {
            Long ingredientId = entry.getKey();
            BigDecimal shortageQuantity = entry.getValue();

            Ingredient ingredient = ingredientMap.get(ingredientId);
            if (ingredient == null) {
                continue;
            }

            BigDecimal requiredQuantity = usageMap.getOrDefault(ingredientId, BigDecimal.ZERO);
            BigDecimal availableQuantity = requiredQuantity.subtract(shortageQuantity);

            for (Long userId : memberUserIds) {
                NotificationPublishCommand command =
                        NotificationPublishCommand.stockShortageDetected(
                                userId,
                                ingredient.getStore().getStorePublicId(),
                                ingredient.getIngredientPublicId(),
                                ingredient.getName(),
                                requiredQuantity,
                                availableQuantity.max(BigDecimal.ZERO),
                                shortageQuantity
                        );

                notificationPublishService.publish(command);
            }
        }
    }
}