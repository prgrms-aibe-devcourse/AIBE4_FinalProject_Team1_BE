package kr.inventory.domain.stock.service;

import kr.inventory.domain.stock.entity.StockShortage;
import kr.inventory.domain.stock.repository.StockShortageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockShortageService {

    private final StockShortageRepository stockShortageRepository;

    @Transactional
    public void recordShortages(Long storeId, Long salesOrderId,
                                Map<Long, BigDecimal> usageMap,
                                Map<Long, BigDecimal> shortageMap) {

        List<StockShortage> shortages = shortageMap.entrySet().stream()
                .map(entry -> {
                    Long ingredientId = entry.getKey();
                    BigDecimal shortageAmount = entry.getValue();
                    BigDecimal requiredAmount = usageMap.get(ingredientId);

                    return StockShortage.createPending(
                            storeId,
                            salesOrderId,
                            ingredientId,
                            requiredAmount,
                            shortageAmount
                    );
                })
                .toList();

        stockShortageRepository.saveAll(shortages);
    }
}