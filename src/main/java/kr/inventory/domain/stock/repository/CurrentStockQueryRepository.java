package kr.inventory.domain.stock.repository;

import kr.inventory.ai.stock.tool.enums.StockOverviewSortBy;
import kr.inventory.ai.stock.tool.enums.StockOverviewStatusFilter;
import kr.inventory.domain.stock.service.command.CurrentStockOverviewSummary;

import java.util.List;

public interface CurrentStockQueryRepository {
    List<CurrentStockOverviewSummary> findCurrentStockOverview(
            Long storeId,
            String keyword,
            StockOverviewStatusFilter status,
            StockOverviewSortBy sortBy,
            Integer limit
    );
}
