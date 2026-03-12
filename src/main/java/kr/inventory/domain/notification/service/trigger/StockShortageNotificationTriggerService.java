package kr.inventory.domain.notification.service.trigger;

import kr.inventory.domain.notification.service.publish.NotificationPublishCommand;
import kr.inventory.domain.notification.service.publish.NotificationPublishService;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.store.service.StoreMemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
            Map<Long, Ingredient> ingredientMap,
            List<Long> newlyShortageIngredientIds
    ) {
        if (newlyShortageIngredientIds == null || newlyShortageIngredientIds.isEmpty()) {
            return;
        }

        List<Ingredient> ingredients = newlyShortageIngredientIds.stream()
                .map(ingredientMap::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(Ingredient::getName))
                .toList();

        if (ingredients.isEmpty()) {
            return;
        }

        Ingredient firstIngredient = ingredients.get(0);
        List<String> ingredientNames = ingredients.stream()
                .map(Ingredient::getName)
                .toList();

        List<Long> memberUserIds = storeMemberQueryService.findActiveMemberUserIds(storeId);

        for (Long userId : memberUserIds) {
            NotificationPublishCommand command =
                    NotificationPublishCommand.stockShortageDetectedGrouped(
                            userId,
                            firstIngredient.getStore().getStorePublicId(),
                            ingredientNames
                    );

            notificationPublishService.publish(command);
        }
    }
}