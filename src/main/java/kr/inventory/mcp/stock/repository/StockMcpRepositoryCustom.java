package kr.inventory.mcp.stock.repository;

import kr.inventory.mcp.stock.dto.response.GetCurrentStockToolResponse;

import java.util.List;

public interface StockMcpRepositoryCustom {
    long countCurrentStockItems(Long storeId, String keyword);

    List<GetCurrentStockToolResponse.Item> findCurrentStockItems(
            Long storeId,
            String keyword,
            int limit
    );
}
