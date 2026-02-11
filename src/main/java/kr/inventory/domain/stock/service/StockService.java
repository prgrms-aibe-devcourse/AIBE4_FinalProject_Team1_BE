package kr.inventory.domain.stock.service;

import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
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

    public Map<Long, BigDecimal> deductStockWithFEFO(Map<Long, BigDecimal> usageMap){
        List<Long> sortedIds = getSortedIngredientIds(usageMap);

        Map<Long, List<IngredientStockBatch>> batchGroup = fetchBatchesGroupedById(sortedIds);

        Map<Long,BigDecimal> shortageMap = new HashMap<>();

        for(Long ingredientId : sortedIds){
            BigDecimal needAmount = usageMap.get(ingredientId);
            List<IngredientStockBatch> batches = batchGroup.getOrDefault(ingredientId, List.of());

            BigDecimal remainingShortage = deductIngredientStock(batches, needAmount);

            if (remainingShortage.signum() > 0) {
                shortageMap.put(ingredientId, remainingShortage);
            }
        }
        return shortageMap;
    }

    private List<Long> getSortedIngredientIds(Map<Long, BigDecimal> usageMap){
        return usageMap.keySet().stream()
                .sorted()
                .toList();
    }

    private Map<Long, List<IngredientStockBatch>> fetchBatchesGroupedById(List<Long> ingredientIds) {
        List<IngredientStockBatch> allBatches = ingredientStockBatchRepository.findAllAvailableBatchesWithLock(ingredientIds);

        return allBatches.stream()
                .collect(Collectors.groupingBy(IngredientStockBatch::getIngredientId));
    }

    private BigDecimal deductIngredientStock(List<IngredientStockBatch> batches, BigDecimal needAmount) {
        BigDecimal remaining = needAmount;

        for (IngredientStockBatch batch : batches) {
            if (remaining.signum() <= 0) break;

            BigDecimal actualDeducted = batch.deductWithClamp(remaining);
            remaining = remaining.subtract(actualDeducted);
        }

        return remaining;
    }
}
