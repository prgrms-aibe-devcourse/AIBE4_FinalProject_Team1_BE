package kr.inventory.domain.stock.service;

import kr.inventory.domain.notification.service.trigger.StockThresholdNotificationTriggerService;
import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.service.command.IngredientStockTotal;
import kr.inventory.domain.stock.service.command.StockDeductionLogCommand;
import kr.inventory.domain.stock.service.command.StockDeductionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StockService {
	private final IngredientStockBatchRepository ingredientStockBatchRepository;
    private final IngredientRepository ingredientRepository;
    private final StockThresholdNotificationTriggerService stockThresholdNotificationTriggerService;
	private final StockLogService stockLogService;

	public Map<Long, BigDecimal> deductStockWithFEFO(StockDeductionRequest request) {
		List<Long> sortedIds = getSortedIngredientIds(request.usageMap());

        Map<Long, BigDecimal> beforeStockMap = getCurrentStockMap(request.storeId(), sortedIds);
        Map<Long, Ingredient> ingredientMap = getIngredientMap(sortedIds);

		Map<Long, List<IngredientStockBatch>> batchGroup = fetchBatchesGroupedById(request.storeId(), sortedIds);

		Map<Long, BigDecimal> shortageMap = new HashMap<>();

		for (Long ingredientId : sortedIds) {
			BigDecimal needAmount = request.usageMap().get(ingredientId);
			List<IngredientStockBatch> batches = batchGroup.getOrDefault(ingredientId, List.of());

			BigDecimal remainingShortage = deductIngredientStock(batches, needAmount, request.salesOrderId());

			if (remainingShortage.signum() > 0) {
				shortageMap.put(ingredientId, remainingShortage);
			}
		}

        Map<Long, BigDecimal> afterStockMap = getCurrentStockMapFromBatches(batchGroup, sortedIds);

        triggerBelowThresholdNotifications(
                request.storeId(),
                sortedIds,
                beforeStockMap,
                afterStockMap,
                ingredientMap
        );

		return shortageMap;
	}

	private List<Long> getSortedIngredientIds(Map<Long, BigDecimal> usageMap) {
		return usageMap.keySet().stream()
			.sorted()
			.toList();
	}

	private Map<Long, List<IngredientStockBatch>> fetchBatchesGroupedById(Long storeId, List<Long> ingredientIds) {
		List<IngredientStockBatch> allBatches = ingredientStockBatchRepository.findAvailableBatchesByStoreWithLock(
			storeId, ingredientIds);

		return allBatches.stream()
			.collect(Collectors.groupingBy(IngredientStockBatch::getIngredientId));
	}

	private BigDecimal deductIngredientStock(List<IngredientStockBatch> batches, BigDecimal needAmount,
		Long salesOrderId) {
		BigDecimal remaining = needAmount;

		for (IngredientStockBatch batch : batches) {
			if (remaining.signum() <= 0)
				break;

			BigDecimal actualDeducted = batch.deductWithClamp(remaining);

			if (actualDeducted.signum() > 0) {
				StockDeductionLogCommand logCommand = StockDeductionLogCommand.forSale(
					batch,
					actualDeducted,
					batch.getRemainingQuantity(),
					salesOrderId
				);

				stockLogService.logDeduction(logCommand);
			}
			remaining = remaining.subtract(actualDeducted);
		}

		return remaining;
	}

    private Map<Long, BigDecimal> getCurrentStockMap(Long storeId, List<Long> ingredientIds) {
        Map<Long, BigDecimal> result = new HashMap<>();

        List<IngredientStockTotal> totals =
                ingredientStockBatchRepository.findTotalRemainingByStoreIdAndIngredientIds(storeId, ingredientIds);

        for (IngredientStockTotal total : totals) {
            result.put(
                    total.ingredientId(),
                    total.totalQuantity() == null ? BigDecimal.ZERO : total.totalQuantity()
            );
        }

        for (Long ingredientId : ingredientIds) {
            result.putIfAbsent(ingredientId, BigDecimal.ZERO);
        }

        return result;
    }

    private Map<Long, BigDecimal> getCurrentStockMapFromBatches(
            Map<Long, List<IngredientStockBatch>> batchGroup,
            List<Long> ingredientIds
    ) {
        Map<Long, BigDecimal> result = new HashMap<>();

        for (Long ingredientId : ingredientIds) {
            BigDecimal total = batchGroup.getOrDefault(ingredientId, List.of()).stream()
                    .map(IngredientStockBatch::getRemainingQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            result.put(ingredientId, total);
        }

        return result;
    }

    private Map<Long, Ingredient> getIngredientMap(List<Long> ingredientIds) {
        return ingredientRepository.findByIngredientIdIn(ingredientIds).stream()
                .collect(Collectors.toMap(Ingredient::getIngredientId, ingredient -> ingredient));
    }

    private void triggerBelowThresholdNotifications(
            Long storeId,
            List<Long> ingredientIds,
            Map<Long, BigDecimal> beforeStockMap,
            Map<Long, BigDecimal> afterStockMap,
            Map<Long, Ingredient> ingredientMap
    ) {
        for (Long ingredientId : ingredientIds) {
            Ingredient ingredient = ingredientMap.get(ingredientId);
            if (ingredient == null) {
                continue;
            }

            BigDecimal threshold = ingredient.getLowStockThreshold();
            if (threshold == null) {
                continue;
            }

            BigDecimal before = beforeStockMap.getOrDefault(ingredientId, BigDecimal.ZERO);
            BigDecimal after = afterStockMap.getOrDefault(ingredientId, BigDecimal.ZERO);

            boolean crossedBelowThreshold =
                    before.compareTo(threshold) >= 0 && after.compareTo(threshold) < 0;

            if (crossedBelowThreshold) {
                stockThresholdNotificationTriggerService.notifyStoreMembersBelowThreshold(
                        storeId,
                        ingredient,
                        after
                );
            }
        }
    }
}
