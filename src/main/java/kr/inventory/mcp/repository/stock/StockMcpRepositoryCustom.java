package kr.inventory.mcp.repository.stock;

import kr.inventory.mcp.dto.stock.response.GetCurrentStockToolResponse;

import java.util.List;

public interface StockMcpRepositoryCustom {
    long countCurrentStockItems(Long storeId, String keyword);

    List<GetCurrentStockToolResponse.Item> findCurrentStockItems(
            Long storeId,
            String keyword,
            int limit
    );
}
