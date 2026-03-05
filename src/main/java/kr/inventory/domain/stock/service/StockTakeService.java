package kr.inventory.domain.stock.service;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.exception.IngredientErrorCode;
import kr.inventory.domain.reference.exception.IngredientException;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.request.StockTakeCreateRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeItemDraftUpdateRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeItemRequest;
import kr.inventory.domain.stock.controller.dto.request.StockTakeItemsDraftUpdateRequest;
import kr.inventory.domain.stock.controller.dto.response.StockTakeDetailResponse;
import kr.inventory.domain.stock.controller.dto.response.StockTakeItemResponse;
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
import java.util.*;
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

    @Transactional(readOnly = true)
    public StockTakeDetailResponse getStockTakeSheetDetail(Long userId, UUID storePublicId, UUID sheetPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        StockTakeSheet sheet = stockTakeSheetRepository.findBySheetPublicIdAndStoreId(sheetPublicId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.SHEET_NOT_FOUND));

        List<StockTake> items = stockTakeRepository.findBySheet(sheet);
        List<StockTakeItemResponse> itemResponses = items.stream()
                .map(StockTakeItemResponse::from)
                .toList();

        return StockTakeDetailResponse.from(sheet, itemResponses);
    }

    @Transactional
    public Long createStockTakeSheet(Long userId, UUID storePublicId, StockTakeCreateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        StockTakeSheet sheet = createAndSaveSheet(storeId, request.title());

        List<StockTake> items = buildDraftItems(storeId, sheet, request.items());

        stockTakeRepository.saveAll(items);

        return sheet.getSheetId();
    }

    private StockTakeSheet createAndSaveSheet(Long storeId, String title) {
        StockTakeSheet sheet = StockTakeSheet.create(storeId, title);
        stockTakeSheetRepository.save(sheet);
        return sheet;
    }

    private List<StockTake> buildDraftItems(Long storeId, StockTakeSheet sheet, List<StockTakeItemRequest> itemRequests) {
        List<UUID> ingredientPublicIds = itemRequests.stream()
                .map(StockTakeItemRequest::ingredientPublicId)
                .toList();

        Map<UUID, Ingredient> ingredientMap = ingredientRepository
                .findAllByStoreStoreIdAndIngredientPublicIdIn(storeId, ingredientPublicIds)
                .stream()
                .collect(Collectors.toMap(Ingredient::getIngredientPublicId, Function.identity()));

        return itemRequests.stream()
                .map(req -> {
                    Ingredient ingredient = Optional.ofNullable(ingredientMap.get(req.ingredientPublicId()))
                            .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));
                    return StockTake.createDraft(sheet, ingredient, req.stockTakeQty());
                })
                .toList();
    }

    @Transactional
    public void updateDraftItems(Long userId, UUID storePublicId, UUID sheetPublicId, StockTakeItemsDraftUpdateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        StockTakeSheet sheet = loadSheetForUpdate(sheetPublicId, storeId);

        List<UUID> ingredientPublicIds = extractDistinctIngredientPublicIds(request);

        List<StockTake> existingItems = loadSheetItemsWithLock(sheet, ingredientPublicIds);
        Map<UUID, StockTake> existingMap = indexByIngredientPublicId(existingItems);

        applyDraftUpdatesOrThrow(request, existingMap);
    }

    private StockTakeSheet loadSheetForUpdate(UUID sheetPublicId, Long storeId) {
        StockTakeSheet sheet = stockTakeSheetRepository.findBySheetPublicIdAndStoreIdWithLock(sheetPublicId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.SHEET_NOT_FOUND));

        validateSheetNotConfirmed(sheet);

        return sheet;
    }

    private void validateSheetNotConfirmed(StockTakeSheet sheet) {
        if (sheet.getStatus() == StockTakeStatus.CONFIRMED) {
            throw new StockException(StockErrorCode.ALREADY_CONFIRMED);
        }
    }

    private List<UUID> extractDistinctIngredientPublicIds(StockTakeItemsDraftUpdateRequest request) {
        return request.items().stream()
                .map(StockTakeItemDraftUpdateRequest::ingredientPublicId)
                .distinct()
                .toList();
    }

    private List<StockTake> loadSheetItemsWithLock(StockTakeSheet sheet, List<UUID> ingredientPublicIds) {
        if (ingredientPublicIds.isEmpty()) return List.of();
        return stockTakeRepository.findAllBySheetAndIngredientPublicIdsWithLock(sheet, ingredientPublicIds);
    }

    private Map<UUID, StockTake> indexByIngredientPublicId(List<StockTake> items) {
        return items.stream().collect(Collectors.toMap(
                it -> it.getIngredient().getIngredientPublicId(),
                Function.identity()
        ));
    }

    private void applyDraftUpdatesOrThrow(StockTakeItemsDraftUpdateRequest request, Map<UUID, StockTake> existingMap) {
        for (StockTakeItemDraftUpdateRequest itemReq : request.items()) {
            UUID ingredientPublicId = itemReq.ingredientPublicId();
            StockTake stockTake = existingMap.get(ingredientPublicId);

            if (stockTake == null) {
                throw new StockException(StockErrorCode.STOCK_TAKE_ITEM_NOT_FOUND);
            }

            stockTake.updateStockTakeQty(itemReq.stockTakeQty());
        }
    }

	@Transactional
	public void confirmSheet(Long userId, UUID storePublicId, UUID sheetPublicId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockTakeSheet sheet = loadSheetForUpdate(sheetPublicId, storeId);

		List<StockTake> items = stockTakeRepository.findBySheet(sheet);
		if (items.isEmpty()) {
			sheet.confirm();
			return;
		}

        Map<Long, List<IngredientStockBatch>> batchMap = loadBatchesGroupedByIngredient(storeId, items);

        applyConfirmToItems(storeId, items, batchMap);

		sheet.confirm();
	}

    private Map<Long, List<IngredientStockBatch>> loadBatchesGroupedByIngredient(Long storeId, List<StockTake> items) {
        List<Long> ingredientIds = items.stream()
                .map(item -> item.getIngredient().getIngredientId())
                .toList();

        return ingredientStockBatchRepository.findAvailableBatchesByStoreWithLock(storeId, ingredientIds)
                .stream()
                .collect(Collectors.groupingBy(IngredientStockBatch::getIngredientId));
    }

    private void applyConfirmToItems(Long storeId, List<StockTake> items, Map<Long, List<IngredientStockBatch>> batchMap) {
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