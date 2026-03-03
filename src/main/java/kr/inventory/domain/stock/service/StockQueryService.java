package kr.inventory.domain.stock.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.inventory.domain.stock.controller.dto.response.StockBatchResponse;
import kr.inventory.domain.stock.controller.dto.response.StockSummaryResponse;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryService {
	private final IngredientStockBatchRepository batchRepository;
	private final StoreAccessValidator storeAccessValidator;

	public List<StockSummaryResponse> getStoreStockList(Long userId, UUID storePublicId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		return batchRepository.findStockSummaryList(storeId);
	}

	public List<StockBatchResponse> getIngredientBatchDetails(Long userId, UUID storePublicId, UUID ingredientId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		// 잔액이 남아있는 배치를 유통기한 순으로 조회
		return batchRepository.findAvailableBatchesByStore(storeId, List.of(ingredientId))
			.stream()
			.map(StockBatchResponse::from)
			.toList();
	}
}
