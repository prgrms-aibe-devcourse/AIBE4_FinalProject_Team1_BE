package kr.inventory.domain.stock.service;

import kr.inventory.ai.stock.tool.enums.StockOverviewSortBy;
import kr.inventory.ai.stock.tool.enums.StockOverviewStatusFilter;
import kr.inventory.domain.stock.repository.CurrentStockQueryRepository;
import kr.inventory.domain.stock.service.command.CurrentStockOverviewSummary;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentStockQueryService{

    private final StoreAccessValidator storeAccessValidator;
    private final CurrentStockQueryRepository currentStockQueryRepository;

    public List<CurrentStockOverviewSummary> getCurrentStockOverview(
            Long userId,
            UUID storePublicId,
            String keyword,
            StockOverviewStatusFilter status,
            StockOverviewSortBy sortBy,
            Integer limit
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        return currentStockQueryRepository.findCurrentStockOverview(
                storeId,
                keyword,
                status,
                sortBy,
                limit
        );
    }
}