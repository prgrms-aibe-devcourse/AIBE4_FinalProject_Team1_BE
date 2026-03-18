package kr.inventory.domain.analytics.service;

import kr.inventory.domain.analytics.controller.dto.request.ESStockLogSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.StockLogAnalyticResponse;
import kr.inventory.domain.analytics.repository.StockLogSearchRepositoryCustom;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockLogAnalyticService {

	private final StockLogSearchRepositoryCustom stockLogSearchRepository;
	private final StoreAccessValidator storeAccessValidator;

	public List<StockLogAnalyticResponse> getStockLogHistory(Long userId, UUID storePublicId,
		ESStockLogSearchRequest request) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		log.info("매장 {}의 재고 이력 조회 시작: {}", storeId, request.keyword());

		return stockLogSearchRepository.searchStockLogs(storeId, request);

	}
}
