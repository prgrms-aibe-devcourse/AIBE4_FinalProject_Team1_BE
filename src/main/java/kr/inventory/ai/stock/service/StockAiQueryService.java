package kr.inventory.ai.stock.service;

import kr.inventory.ai.stock.tool.dto.response.LowStockIngredientResponse;
import kr.inventory.domain.stock.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockAiQueryService {
    private final StockQueryService stockQueryService;

    public List<LowStockIngredientResponse> getLowStockIngredients(Long userId, UUID storePublicId) {
        return stockQueryService.getLowStockIngredients(userId, storePublicId);
    }
}