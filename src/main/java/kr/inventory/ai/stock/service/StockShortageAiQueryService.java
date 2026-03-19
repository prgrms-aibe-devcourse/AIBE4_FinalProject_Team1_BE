package kr.inventory.ai.stock.service;

import kr.inventory.ai.stock.tool.dto.response.ShortageRelatedOrderToolResponse;
import kr.inventory.ai.stock.tool.dto.response.StockShortageSummaryItemToolResponse;
import kr.inventory.ai.stock.tool.dto.response.StockShortageSummaryToolResponse;
import kr.inventory.domain.analytics.controller.dto.request.ESStockShortageSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.StockShortageSummaryResponse;
import kr.inventory.domain.analytics.service.StockShortageAnalyticService;
import kr.inventory.domain.stock.entity.StockShortage;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.StockShortageRepository;
import kr.inventory.domain.stock.service.command.ShortageRelatedOrderQueryResult;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockShortageAiQueryService {

	private final StockShortageAnalyticService stockShortageAnalyticService;
	private final StoreAccessValidator storeAccessValidator;
	private final StockShortageRepository stockShortageRepository;

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

	public ShortageRelatedOrderToolResponse getShortageRelatedOrder(
		Long userId,
		UUID storePublicId,
		UUID shortagePublicId
	) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		StockShortage shortage = stockShortageRepository
			.findByStockShortagePublicIdAndStoreId(shortagePublicId, storeId)
			.orElseThrow(() -> new StockException(StockErrorCode.SHORTAGE_NOT_FOUND));

		ShortageRelatedOrderQueryResult result = stockShortageRepository
			.findShortageRelatedOrder(storeId, shortage.getStockShortageId())
			.orElseThrow(() -> new StockException(StockErrorCode.SHORAGE_LINK_ORDER_NOT_FOUND));

		return ShortageRelatedOrderToolResponse.from(result);
	}

	private String normalizeKeyword(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		return keyword.trim();
	}

	private StockShortageSummaryItemToolResponse toToolResponse(StockShortageSummaryResponse response) {
		return new StockShortageSummaryItemToolResponse(
			response.stockShortagePublicId(),
			response.ingredientName(),
			response.totalShortageAmount(),
			response.affectedOrderCount(),
			response.lastOccurrenceTime()
		);
	}
}