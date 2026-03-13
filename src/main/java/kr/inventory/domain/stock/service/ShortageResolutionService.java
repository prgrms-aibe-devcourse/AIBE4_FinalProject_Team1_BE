package kr.inventory.domain.stock.service;

import kr.inventory.domain.stock.entity.StockShortage;
import kr.inventory.domain.stock.entity.enums.ShortageStatus;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StockShortageRepository;
import kr.inventory.domain.stock.repository.dto.IngredientStockTotalDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShortageResolutionService {

    private final StockShortageRepository stockShortageRepository;
    private final IngredientStockBatchRepository ingredientStockBatchRepository;

    public void closePendingShortagesIfStockRecovered(Long storeId, Long ingredientId) {
        closePendingShortagesIfStockRecovered(storeId, List.of(ingredientId));
    }

    public void closePendingShortagesIfStockRecovered(Long storeId, Collection<Long> ingredientIds) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            return;
        }

        Set<Long> distinctIngredientIds = ingredientIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (distinctIngredientIds.isEmpty()) {
            return;
        }

        Map<Long, BigDecimal> stockMap = ingredientStockBatchRepository
                .calculateTotalQuantities(storeId, distinctIngredientIds)
                .stream()
                .collect(Collectors.toMap(
                        IngredientStockTotalDto::ingredientId,
                        dto -> dto.totalQuantity() == null ? BigDecimal.ZERO : dto.totalQuantity()
                ));

        List<StockShortage> pendingShortages = stockShortageRepository.findPendingShortages(
                storeId,
                distinctIngredientIds,
                ShortageStatus.PENDING
        );

        Map<Long, List<StockShortage>> shortagesByIngredient = pendingShortages.stream()
                .collect(Collectors.groupingBy(StockShortage::getIngredientId));

        for (Long ingredientId : distinctIngredientIds) {
            BigDecimal currentStock = stockMap.getOrDefault(ingredientId, BigDecimal.ZERO);
            List<StockShortage> shortages = shortagesByIngredient.getOrDefault(ingredientId, Collections.emptyList());

            if (currentStock.signum() <= 0) {
                continue;
            }

            for (StockShortage shortage : shortages) {
                if (currentStock.compareTo(shortage.getShortageAmount()) >= 0) {
                    shortage.close();
                }
            }
        }
    }
}