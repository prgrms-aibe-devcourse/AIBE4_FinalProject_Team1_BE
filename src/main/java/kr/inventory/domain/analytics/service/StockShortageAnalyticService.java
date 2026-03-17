package kr.inventory.domain.analytics.service;

import kr.inventory.domain.analytics.controller.dto.request.ESStockShortageSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.StockShortageSummaryResponse;
import kr.inventory.domain.analytics.repository.StockShortageSearchRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockShortageAnalyticService {

    private final StockShortageSearchRepository stockShortageSearchRepository;
	private final StoreAccessValidator storeAccessValidator;

	public List<StockShortageSummaryResponse> getStockShortageSummary(Long userId, UUID storePublicId,
		ESStockShortageSearchRequest request) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		log.info("매장 {}의 재고 부족 분 집계 시작: {}", storeId, request.keyword());

		return stockShortageSearchRepository.getShortageSummary(storeId, request);

	}
}
