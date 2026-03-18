package kr.inventory.ai.stock.service;

import kr.inventory.ai.stock.tool.dto.request.CurrentStockOverviewToolRequest;
import kr.inventory.ai.stock.tool.dto.response.CurrentStockOverviewItemToolResponse;
import kr.inventory.ai.stock.tool.dto.response.CurrentStockOverviewToolResponse;
import kr.inventory.domain.stock.service.CurrentStockQueryService;
import kr.inventory.domain.stock.service.command.CurrentStockOverviewSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentStockAiQueryService {

    private final CurrentStockQueryService currentStockQueryService;

    public CurrentStockOverviewToolResponse getCurrentStockOverview(
            Long userId,
            UUID storePublicId,
            CurrentStockOverviewToolRequest request
    ) {
        List<CurrentStockOverviewSummary> summaries =
                currentStockQueryService.getCurrentStockOverview(
                        userId,
                        storePublicId,
                        request.normalizedKeyword(),
                        request.status(),
                        request.resolvedSortBy(),
                        request.resolvedLimit()
                );

        List<CurrentStockOverviewItemToolResponse> items = summaries.stream()
                .map(CurrentStockOverviewItemToolResponse::from)
                .toList();

        return CurrentStockOverviewToolResponse.of(items);
    }
}