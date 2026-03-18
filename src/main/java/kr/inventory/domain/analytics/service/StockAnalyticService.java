package kr.inventory.domain.analytics.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

	public List<StockAnalyticResponse> getIntegratedAnalysis(Long userId, UUID storePublicId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		log.debug("[Analytics] 재고/폐기 통합 분석 집계 storeId={}", storeId);

		Map<Long, StockAnalyticResponse.StockPart> stockMap = stockSearchRepository.aggregateStockMap(storeId);
		Map<Long, StockAnalyticResponse.WastePart> wasteMap = stockSearchRepository.aggregateWasteMap(storeId);

		// 2. ID 통합 및 데이터 병합
		Set<Long> allIds = new HashSet<>(stockMap.keySet());
		allIds.addAll(wasteMap.keySet());

		return allIds.stream()
			.map(id -> {
				StockAnalyticResponse.StockPart stock = stockMap.getOrDefault(id,
					StockAnalyticResponse.StockPart.empty());
				StockAnalyticResponse.WastePart waste = wasteMap.getOrDefault(id,
					StockAnalyticResponse.WastePart.empty());

				// DTO의 정적 팩토리 메서드 호출 (unit은 Response 내부에서 처리하거나 파라미터로 전달)
				return StockAnalyticResponse.of(id, stock, waste);
			})
			.toList();
	}

}