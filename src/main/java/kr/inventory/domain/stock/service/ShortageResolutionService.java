package kr.inventory.domain.stock.service;

import kr.inventory.domain.stock.entity.StockShortage;
import kr.inventory.domain.stock.entity.enums.ShortageStatus;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StockShortageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShortageResolutionService {

    private final StockShortageRepository stockShortageRepository;
    private final IngredientStockBatchRepository ingredientStockBatchRepository;

    public void resolveIfFilled(Long storeId, Long ingredientId) {
        BigDecimal currentStock = ingredientStockBatchRepository.calculateTotalQuantity(storeId, ingredientId);
        if (currentStock == null) {
            currentStock = BigDecimal.ZERO;
        }

        List<StockShortage> pendingShortages =
                stockShortageRepository.findByStoreIdAndIngredientIdAndStatusOrderByCreatedAtAsc(
                        storeId, ingredientId, ShortageStatus.PENDING
                );

        for (StockShortage shortage : pendingShortages) {
            if (currentStock.compareTo(shortage.getShortageAmount()) >= 0) {
                shortage.resolve();
            }
        }
    }

    public void resolveIfFilled(Long storeId, Collection<Long> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return;
        }

        for (Long ingredientId : ingredientIds) {
            resolveIfFilled(storeId, ingredientId);
        }
    }
}