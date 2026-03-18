package kr.inventory.ai.stock.service;

import kr.inventory.ai.stock.tool.dto.request.SearchStockLogsToolRequest;
import kr.inventory.ai.stock.tool.dto.response.SearchStockLogsToolResponse;
import kr.inventory.ai.stock.tool.dto.response.StockLogItemToolResponse;
import kr.inventory.domain.analytics.controller.dto.request.ESStockLogSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.StockLogAnalyticResponse;
import kr.inventory.domain.analytics.service.StockLogAnalyticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockLogAiQueryService {

    private final StockLogAnalyticService stockLogAnalyticService;

    public SearchStockLogsToolResponse searchStockLogs(
            Long userId,
            UUID storePublicId,
            SearchStockLogsToolRequest request
    ) {
        ESStockLogSearchRequest esRequest = new ESStockLogSearchRequest(
                request.normalizedKeyword(),
                request.normalizedTransactionType(),
                request.normalizedReferenceType(),
                request.from(),
                request.to()
        );

        List<StockLogAnalyticResponse> results =
                stockLogAnalyticService.getStockLogHistory(userId, storePublicId, esRequest);

        List<StockLogItemToolResponse> logs = results.stream()
                .map(StockLogItemToolResponse::from)
                .toList();

        return new SearchStockLogsToolResponse(
                logs.size(),
                logs
        );
    }
}
