package kr.inventory.domain.analytics.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import kr.inventory.domain.analytics.controller.dto.request.ESStockLogSearchRequest;
import kr.inventory.domain.analytics.controller.dto.request.ESStockShortageSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.StockLogAnalyticResponse;
import kr.inventory.domain.analytics.controller.dto.response.StockShortageSummaryResponse;
import kr.inventory.domain.analytics.repository.StockLogSearchRepositoryCustom;
import kr.inventory.domain.analytics.repository.StockShortageSearchRepositoryCustom;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockShortageAnalyticService {

	private StockShortageSearchRepositoryCustom stockShortageSearchRepositoryCustom;
	private StoreAccessValidator storeAccessValidator;

	public List<StockShortageSummaryResponse> getStockShortageSummary(Long userId, UUID storePublicId,
		ESStockShortageSearchRequest request) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		log.info("매장 {}의 재고 부족 분 집계 시작: {}", storeId, request.keyword());

		return stockShortageSearchRepositoryCustom.getShortageSummary(storeId, request);

	}
}
