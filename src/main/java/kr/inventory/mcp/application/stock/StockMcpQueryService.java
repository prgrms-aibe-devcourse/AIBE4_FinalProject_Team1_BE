package kr.inventory.mcp.application.stock;

import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.mcp.dto.stock.request.GetCurrentStockToolRequest;
import kr.inventory.mcp.dto.stock.response.GetCurrentStockToolResponse;
import kr.inventory.mcp.repository.stock.StockMcpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockMcpQueryService {

    private static final int STOCK_LIST_LIMIT = 20;

    private final StoreAccessValidator storeAccessValidator;
    private final StockMcpRepository stockMcpRepository;

    public GetCurrentStockToolResponse getCurrentStockStatus(Long userId, GetCurrentStockToolRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, request.storePublicId());

        long totalCount = stockMcpRepository.countCurrentStockItems(storeId, request.keyword());

        return new GetCurrentStockToolResponse(
                Math.toIntExact(totalCount),
                stockMcpRepository.findCurrentStockItems(storeId, request.keyword(), STOCK_LIST_LIMIT)
        );
    }
}