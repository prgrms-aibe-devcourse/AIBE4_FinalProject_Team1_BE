package kr.inventory.domain.analytics.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;

import kr.inventory.domain.analytics.controller.dto.response.StockAnalyticResponse;
import kr.inventory.domain.analytics.document.stock.IngredientStockBatchDocument;

import kr.inventory.domain.analytics.repository.StockBatchSearchRepositoryCustom;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnalyticService {

	private final StoreAccessValidator storeAccessValidator;
	private final StockBatchSearchRepositoryCustom stockSearchRepository;

	public List<IngredientStockBatchDocument> searchStockBatches(
		Long userId, UUID storePublicId, String keyword, String status, Integer daysUntilExpiry
	) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		return stockSearchRepository.searchStockBatches(storeId, keyword, status, daysUntilExpiry);
	}

	@Cacheable(value = "stock:integrated-analysis", key = "#storePublicId")
	public List<StockAnalyticResponse> getIntegratedAnalysis(Long userId, UUID storePublicId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		log.debug("[Analytics] 재고/폐기 통합 분석 집계 storeId={}", storeId);

		Map<Long, StockAnalyticResponse.StockPart> stockMap = stockSearchRepository.aggregateStockMap(storeId);
		Map<Long, StockAnalyticResponse.WastePart> wasteMap = stockSearchRepository.aggregateWasteMap(storeId);

		// 2. ID 통합 및 데이터 병합
		Set<Long> allIds = new HashSet<>(stockMap.keySet());
		allIds.addAll(wasteMap.keySet());

		return allIds.stream()
			.map(id -> StockAnalyticResponse.of(
				id,
				stockMap.getOrDefault(id, StockAnalyticResponse.StockPart.empty()),
				wasteMap.getOrDefault(id, StockAnalyticResponse.WastePart.empty())
			))
			.toList();
	}
}