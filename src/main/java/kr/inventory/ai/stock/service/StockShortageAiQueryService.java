package kr.inventory.ai.stock.service;

import kr.inventory.ai.stock.tool.dto.response.StockShortageSummaryItemToolResponse;
import kr.inventory.ai.stock.tool.dto.response.StockShortageSummaryToolResponse;
import kr.inventory.domain.analytics.controller.dto.request.ESStockShortageSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.StockShortageSummaryResponse;
import kr.inventory.domain.analytics.service.StockShortageAnalyticService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockShortageAiQueryService {

	private final StockShortageAnalyticService stockShortageAnalyticService;

	public StockShortageSummaryToolResponse getStockShortageSummary(
		Long userId,
		UUID storePublicId,
		String keyword,
		String status,
		OffsetDateTime from,
		OffsetDateTime to
	) {
		ESStockShortageSearchRequest analyticRequest =
			new ESStockShortageSearchRequest(
				normalizeKeyword(keyword),
				from,
				to,
				status
			);

		List<StockShortageSummaryResponse> results =
			stockShortageAnalyticService.getStockShortageSummary(userId, storePublicId, analyticRequest);

		List<StockShortageSummaryItemToolResponse> items = results.stream()
			.map(this::toToolResponse)
			.toList();

		return new StockShortageSummaryToolResponse(items.size(), items);
	}

	private String normalizeKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		return keyword.trim();
	}

	private StockShortageSummaryItemToolResponse toToolResponse(StockShortageSummaryResponse response) {
		return new StockShortageSummaryItemToolResponse(
			response.ingredientId(),
			response.ingredientName(),
			response.totalShortageAmount(),
			response.affectedOrderCount(),
			response.lastOccurrenceTime(),
			response.relatedOrderIds()
		);
	}
}