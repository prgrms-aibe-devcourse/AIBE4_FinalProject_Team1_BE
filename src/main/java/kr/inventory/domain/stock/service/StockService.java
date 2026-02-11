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

@Service
@RequiredArgsConstructor
@Transactional
public class StockService {
    private final IngredientStockBatchRepository ingredientStockBatchRepository;

    public Map<Long, BigDecimal> deductStockWithFEFO(Map<Long, BigDecimal> usageMap){
        Map<Long,BigDecimal> shortageMap = new HashMap<>();

        for(Map.Entry<Long,BigDecimal> entry : usageMap.entrySet()){
            Long ingredientId = entry.getKey();
            BigDecimal needAmount = entry.getValue();

            BigDecimal remainingShortage = deductSingleIngredient(ingredientId, needAmount);

            if (remainingShortage.signum() > 0) {
                shortageMap.put(ingredientId, remainingShortage);
                // TODO: 재고가 0 이하로 떨어질 경우 알림 발송 기능 구현 필요
            }
        }
        return shortageMap;
    }

    private BigDecimal deductSingleIngredient(Long ingredientId, BigDecimal needAmount) {
        List<IngredientStockBatch> batches = ingredientStockBatchRepository.findAvailableBatchesWithLock(ingredientId);

        for (IngredientStockBatch batch : batches) {
            if (needAmount.signum() <= 0) break;

            BigDecimal actualDeducted = batch.deductWithClamp(needAmount);
            needAmount = needAmount.subtract(actualDeducted);
        }

        return needAmount;
    }
}
