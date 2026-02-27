package kr.inventory.domain.stock.service;

import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.catalog.exception.IngredientErrorCode;
import kr.inventory.domain.catalog.exception.IngredientException;
import kr.inventory.domain.catalog.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.request.StockTakeCreateRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeItemRequest;
import kr.inventory.domain.stock.controller.dto.response.StockTakeSheetResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockTake;
import kr.inventory.domain.stock.entity.StockTakeSheet;
import kr.inventory.domain.stock.entity.enums.StockTakeStatus;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StockTakeRepository;
import kr.inventory.domain.stock.repository.StockTakeSheetRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockTakeService {
	private final StockTakeRepository stockTakeRepository;
	private final IngredientStockBatchRepository ingredientStockBatchRepository;
	private final IngredientRepository ingredientRepository;
	private final StockTakeSheetRepository stockTakeSheetRepository;
	private final StoreAccessValidator storeAccessValidator;

	@Transactional(readOnly = true)
	public List<StockTakeSheetResponse> getStockTakeSheets(Long userId, UUID storePublicId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		List<StockTakeSheet> sheets = stockTakeSheetRepository.findAllByStoreIdOrderByCreatedAtDesc(storeId);
		return sheets.stream()
			.map(StockTakeSheetResponse::from)
			.toList();
	}

	@Transactional
	public Long createStockTakeSheet(Long userId, UUID storePublicId, StockTakeCreateRequest request) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		StockTakeSheet sheet = StockTakeSheet.create(storeId, request.title());
		stockTakeSheetRepository.save(sheet);

		List<Long> ingredientIds = request.items().stream().map(StockTakeItemRequest::ingredientId).toList();

		Map<Long, Ingredient> ingredientMap = ingredientRepository.findAllByStoreStoreIdAndIngredientIdIn(storeId,
			ingredientIds).stream().collect(Collectors.toMap(Ingredient::getIngredientId, Function.identity()));

		List<StockTake> items = request.items().stream()
			.map(req -> {
				Ingredient ingredient = Optional.ofNullable(ingredientMap.get(req.ingredientId()))
					.orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));
				return kr.inventory.domain.stock.entity.StockTake.createDraft(sheet, ingredient, req.stocktakeQty());
			})
			.toList();

		stockTakeRepository.saveAll(items);
		return sheet.getSheetId();
	}

	@Transactional
	public void confirmSheet(Long userId, UUID storePublicId, Long sheetId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		StockTakeSheet sheet = getValidSheetForConfirm(sheetId, storeId);

		List<StockTake> items = stockTakeRepository.findBySheet(sheet);
		if (items.isEmpty()) {
			sheet.confirm();
			return;
		}

		Map<Long, List<IngredientStockBatch>> batchMap = fetchGroupedBatches(storeId, items);

		processStocktakeItems(storeId, items, batchMap);

		sheet.confirm();
	}

	private StockTakeSheet getValidSheetForConfirm(Long sheetId, Long storeId) {
		StockTakeSheet sheet = stockTakeSheetRepository.findByIdAndStoreIdWithLock(sheetId, storeId)
			.orElseThrow(() -> new StockException(StockErrorCode.SHEET_NOT_FOUND));

		if (sheet.getStatus() == StockTakeStatus.CONFIRMED) {
			throw new StockException(StockErrorCode.ALREADY_CONFIRMED);
		}
		return sheet;
	}

	private Map<Long, List<IngredientStockBatch>> fetchGroupedBatches(Long storeId, List<StockTake> items) {
		List<Long> ingredientIds = items.stream()
			.map(item -> item.getIngredient().getIngredientId())
			.toList();

		return ingredientStockBatchRepository.findAvailableBatchesByStoreWithLock(storeId, ingredientIds)
			.stream()
			.collect(Collectors.groupingBy(IngredientStockBatch::getIngredientId));
	}

	private void processStocktakeItems(Long storeId, List<StockTake> items,
		Map<Long, List<IngredientStockBatch>> batchMap) {
		for (StockTake item : items) {
			Long ingredientId = item.getIngredient().getIngredientId();
			List<IngredientStockBatch> ingredientBatches = batchMap.getOrDefault(ingredientId, List.of());

			confirmIndividualItem(storeId, item, ingredientBatches);
		}
	}

	private void confirmIndividualItem(Long storeId, StockTake item, List<IngredientStockBatch> batches) {
		Ingredient ingredient = item.getIngredient();

		BigDecimal theoreticalQty = batches.stream()
			.map(IngredientStockBatch::getRemainingQuantity)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal varianceQty = item.getStockTakeQty().subtract(theoreticalQty);

		item.updateQuantities(theoreticalQty, varianceQty);

		adjustStockByVariance(storeId, ingredient, batches, varianceQty);
	}

	private void adjustStockByVariance(Long storeId, Ingredient ingredient, List<IngredientStockBatch> batches,
		BigDecimal variance) {
		int compare = variance.signum();

		if (compare < 0) {
			handleStockDeficit(batches, variance.abs());
		} else if (compare > 0) {
			createAdjustmentBatch(storeId, ingredient, variance);
		}
	}

	private void handleStockDeficit(List<IngredientStockBatch> batches, BigDecimal deficitAmount) {
		BigDecimal remainingToDeduct = deficitAmount;

		for (IngredientStockBatch batch : batches) {
			if (remainingToDeduct.signum() <= 0)
				break;

			BigDecimal currentRemaining = batch.getRemainingQuantity();
			BigDecimal deductAmount = currentRemaining.min(remainingToDeduct);

			batch.updateRemaining(currentRemaining.subtract(deductAmount));

			remainingToDeduct = remainingToDeduct.subtract(deductAmount);
		}
	}

	private void createAdjustmentBatch(Long storeId, Ingredient ingredient, BigDecimal amount) {
		BigDecimal adjustmentUnitCost = ingredientStockBatchRepository
			.findLatestUnitCostByStoreAndIngredient(storeId, ingredient.getIngredientId())
			.orElseGet(() -> {
				log.warn("재고 조정 중 단가를 찾을 수 없습니다. (식재료: {}, 매장: {}). 단가 0으로 생성됩니다.",
					ingredient.getName(), storeId);
				return BigDecimal.ZERO;
			});

		IngredientStockBatch adjustmentBatch = IngredientStockBatch.createAdjustment(
			storeId,
			ingredient,
			amount,
			adjustmentUnitCost
		);

		ingredientStockBatchRepository.save(adjustmentBatch);
	}
}
