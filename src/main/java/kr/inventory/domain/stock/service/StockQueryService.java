package kr.inventory.domain.stock.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.inventory.domain.stock.controller.dto.request.StockSearchRequest;
import kr.inventory.domain.stock.controller.dto.response.StockBatchResponse;
import kr.inventory.domain.stock.controller.dto.response.StockSummaryResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockQueryService {
	private final IngredientStockBatchRepository batchRepository;
	private final StoreAccessValidator storeAccessValidator;

	public Page<StockSummaryResponse> getStoreStockList(Long userId, UUID storePublicId, StockSearchRequest condition,
		Pageable pageable) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		return batchRepository.findStockSummaryList(storeId, condition, pageable);
	}

	public List<StockBatchResponse> getIngredientBatchDetails(Long userId, UUID storePublicId,
		UUID ingredientPublicId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		List<IngredientStockBatch> batches = batchRepository.findAvailableBatchesByStore(storeId, ingredientPublicId);

		return batches.stream()
			.map(StockBatchResponse::from)
			.toList();
	}
}
