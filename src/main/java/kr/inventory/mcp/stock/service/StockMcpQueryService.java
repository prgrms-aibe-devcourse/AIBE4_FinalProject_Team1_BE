package kr.inventory.mcp.stock.service;

import kr.inventory.domain.stock.controller.dto.response.LowStockIngredientResponse;
import kr.inventory.domain.stock.service.StockQueryService;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.mcp.stock.dto.request.GetCurrentStockToolRequest;
import kr.inventory.mcp.stock.dto.response.GetCurrentStockToolResponse;
import kr.inventory.mcp.stock.repository.StockMcpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockMcpQueryService {

    private static final int STOCK_LIST_LIMIT = 20;

    private final StoreAccessValidator storeAccessValidator;
    private final StockMcpRepository stockMcpRepository;
    private final StockQueryService stockQueryService;

    public GetCurrentStockToolResponse getCurrentStockStatus(Long userId, GetCurrentStockToolRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, request.storePublicId());

        long totalCount = stockMcpRepository.countCurrentStockItems(storeId, request.keyword());

        return new GetCurrentStockToolResponse(
                totalCount,
                stockMcpRepository.findCurrentStockItems(storeId, request.keyword(), STOCK_LIST_LIMIT)
        );
    }

    public List<LowStockIngredientResponse> getLowStockIngredients(Long userId, UUID storePublicId) {
        return stockQueryService.getLowStockIngredients(userId, storePublicId);
    }
}