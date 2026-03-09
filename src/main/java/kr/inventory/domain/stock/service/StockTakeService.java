package kr.inventory.domain.stock.service;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.exception.IngredientErrorCode;
import kr.inventory.domain.reference.exception.IngredientException;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.request.*;
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
import kr.inventory.domain.stock.service.command.StockDeductionLogCommand;
import kr.inventory.domain.stock.service.command.StockInboundLogCommand;
import kr.inventory.domain.stock.service.command.StockTakeConfirmContext;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final StockLogService stockLogService;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<StockTakeSheetResponse> getStockTakeSheets(
            Long userId,
            UUID storePublicId,
            StockTakeSheetSearchRequest request,
            Pageable pageable
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        validateSearchRange(request);

        Page<StockTakeSheet> page = stockTakeSheetRepository
                .searchStockTakeSheets(storeId, request, pageable);

        Page<StockTakeSheetResponse> responsePage =
                page.map(StockTakeSheetResponse::from);

        return PageResponse.from(responsePage);
    }

    private void validateSearchRange(StockTakeSheetSearchRequest request) {
        if (request.from() != null && request.to() != null && request.from().isAfter(request.to())) {
            throw new IllegalArgumentException("조회 시작일시는 종료일시보다 늦을 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public StockTakeDetailResponse getStockTakeSheetDetail(Long userId, UUID storePublicId, UUID sheetPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockTakeSheet sheet = stockTakeSheetRepository.findBySheetPublicIdAndStoreId(sheetPublicId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.SHEET_NOT_FOUND));

        List<StockTake> items = stockTakeRepository.findBySheet(sheet);

        return StockTakeDetailResponse.from(sheet, items.stream().map(StockTakeItemResponse::from).toList());
    }

    @Transactional
    public UUID createStockTakeSheet(Long userId, UUID storePublicId, StockTakeSheetCreateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        validateDuplicateIngredients(request.items());

        StockTakeSheet sheet = createAndSaveSheet(storeId, request.title());
        List<StockTake> items = buildDraftItems(storeId, sheet, request.items());

        stockTakeRepository.saveAll(items);

        return sheet.getSheetPublicId();
    }

    @Transactional
    public void saveStockTakeDraft(Long userId, UUID storePublicId, UUID sheetPublicId, StockTakeDraftSaveRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        validateDuplicateIngredients(request.items());

        StockTakeSheet sheet = loadSheetForUpdate(sheetPublicId, storeId);
        sheet.updateTitle(request.title());

        List<UUID> ingredientPublicIds = extractDistinctIngredientPublicIds(request.items());
        List<StockTake> existingItems = loadSheetItemsWithLock(sheet, ingredientPublicIds);
        Map<UUID, StockTake> existingMap = indexByIngredientPublicId(existingItems);

        applyStockTakeQtyUpdates(request.items(), existingMap);
    }

    @Transactional
    public void confirmStockTakeSheet(Long userId, UUID storePublicId, UUID sheetPublicId, StockTakeConfirmRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        validateDuplicateIngredients(request.items());

        Store store = storeRepository.getReferenceById(storeId);
        User user = userRepository.getReferenceById(userId);

        StockTakeSheet sheet = loadSheetForUpdate(sheetPublicId, storeId);
        sheet.updateTitle(request.title());

        List<UUID> ingredientPublicIds = extractDistinctIngredientPublicIds(request.items());
        List<StockTake> items = loadSheetItemsWithLock(sheet, ingredientPublicIds);

        validateConfirmItemsMatchSheet(sheet, request.items(), items);

        Map<UUID, StockTake> existingMap = indexByIngredientPublicId(items);
        applyStockTakeQtyUpdates(request.items(), existingMap);

        if (items.isEmpty()) {
            sheet.confirm();
            return;
        }

        StockTakeConfirmContext ctx = new StockTakeConfirmContext(
                storeId,
                store,
                user,
                sheet.getSheetId()
        );

        Map<Long, List<IngredientStockBatch>> batchMap = loadBatchesGroupedByIngredient(storeId, items);

        applyConfirmToItems(ctx, items, batchMap);

        sheet.confirm();
    }

    private void validateDuplicateIngredients(List<StockTakeItemQuantityRequest> itemRequests) {
        List<UUID> ingredientPublicIds = itemRequests.stream()
                .map(StockTakeItemQuantityRequest::ingredientPublicId)
                .toList();

        if (ingredientPublicIds.size() != new LinkedHashSet<>(ingredientPublicIds).size()) {
            throw new StockException(StockErrorCode.DUPLICATED_STOCK_TAKE_ITEM);
        }
    }

    private StockTakeSheet createAndSaveSheet(Long storeId, String title) {
        StockTakeSheet sheet = StockTakeSheet.create(storeId, title);
        stockTakeSheetRepository.save(sheet);
        return sheet;
    }

    private List<StockTake> buildDraftItems(Long storeId, StockTakeSheet sheet, List<StockTakeItemQuantityRequest> itemRequests) {
        List<UUID> ingredientPublicIds = extractDistinctIngredientPublicIds(itemRequests);

        Map<UUID, Ingredient> ingredientMap = ingredientRepository
                .findAllByStoreStoreIdAndIngredientPublicIdInAndStatusNot(
                        storeId,
                        ingredientPublicIds,
                        IngredientStatus.DELETED
                )
                .stream()
                .collect(Collectors.toMap(Ingredient::getIngredientPublicId, Function.identity()));

        List<Ingredient> ingredients = itemRequests.stream()
                .map(req -> getIngredientOrThrow(ingredientMap, req.ingredientPublicId()))
                .toList();

        Map<Long, BigDecimal> theoreticalQtyMap = loadTheoreticalQtyMap(storeId, ingredients);

        return itemRequests.stream()
                .map(req -> {
                    Ingredient ingredient = getIngredientOrThrow(ingredientMap, req.ingredientPublicId());

                    BigDecimal theoreticalQty = theoreticalQtyMap.getOrDefault(
                            ingredient.getIngredientId(),
                            BigDecimal.ZERO
                    );

                    return StockTake.createDraft(
                            sheet,
                            ingredient,
                            theoreticalQty,
                            req.stockTakeQty()
                    );
                })
                .toList();
    }

    private Ingredient getIngredientOrThrow(Map<UUID, Ingredient> ingredientMap, UUID ingredientPublicId) {
        return Optional.ofNullable(ingredientMap.get(ingredientPublicId))
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));
    }

    private Map<Long, BigDecimal> loadTheoreticalQtyMap(Long storeId, List<Ingredient> ingredients) {
        List<Long> ingredientIds = ingredients.stream()
                .map(Ingredient::getIngredientId)
                .distinct()
                .toList();

        Map<Long, BigDecimal> qtyMap = ingredientStockBatchRepository
                .findAvailableBatchesByStoreWithLock(storeId, ingredientIds)
                .stream()
                .collect(Collectors.groupingBy(
                        IngredientStockBatch::getIngredientId,
                        Collectors.mapping(
                                IngredientStockBatch::getRemainingQuantity,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                        )
                ));

        for (Ingredient ingredient : ingredients) {
            qtyMap.putIfAbsent(ingredient.getIngredientId(), BigDecimal.ZERO);
        }

        return qtyMap;
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

    private List<UUID> extractDistinctIngredientPublicIds(List<StockTakeItemQuantityRequest> items) {
        return items.stream()
                .map(StockTakeItemQuantityRequest::ingredientPublicId)
                .distinct()
                .toList();
    }

    private List<StockTake> loadSheetItemsWithLock(StockTakeSheet sheet, List<UUID> ingredientPublicIds) {
        if (ingredientPublicIds.isEmpty()) {
            return List.of();
        }
        return stockTakeRepository.findAllBySheetAndIngredientPublicIdsWithLock(sheet, ingredientPublicIds);
    }

    private void validateConfirmItemsMatchSheet(
            StockTakeSheet sheet,
            List<StockTakeItemQuantityRequest> requestItems,
            List<StockTake> lockedItems
    ) {
        Set<UUID> requestIngredientIds = requestItems.stream()
                .map(StockTakeItemQuantityRequest::ingredientPublicId)
                .collect(Collectors.toSet());

        Set<UUID> sheetIngredientIds = stockTakeRepository.findBySheet(sheet).stream()
                .map(item -> item.getIngredient().getIngredientPublicId())
                .collect(Collectors.toSet());

        Set<UUID> lockedIngredientIds = lockedItems.stream()
                .map(item -> item.getIngredient().getIngredientPublicId())
                .collect(Collectors.toSet());

        if (!requestIngredientIds.equals(sheetIngredientIds)) {
            throw new StockException(StockErrorCode.INVALID_STOCK_TAKE_CONFIRM_REQUEST);
        }

        if (!lockedIngredientIds.equals(sheetIngredientIds)) {
            throw new StockException(StockErrorCode.INVALID_STOCK_TAKE_CONFIRM_REQUEST);
        }
    }

    private Map<UUID, StockTake> indexByIngredientPublicId(List<StockTake> items) {
        return items.stream().collect(Collectors.toMap(
                item -> item.getIngredient().getIngredientPublicId(),
                Function.identity()
        ));
    }

    private void applyStockTakeQtyUpdates(
            List<StockTakeItemQuantityRequest> requestedItems,
            Map<UUID, StockTake> existingMap
    ) {
        for (StockTakeItemQuantityRequest requestedItem : requestedItems) {
            UUID ingredientPublicId = requestedItem.ingredientPublicId();
            StockTake stockTake = existingMap.get(ingredientPublicId);

            if (stockTake == null) {
                throw new StockException(StockErrorCode.STOCK_TAKE_ITEM_NOT_FOUND);
            }

            stockTake.updateStockTakeQty(requestedItem.stockTakeQty());
        }
    }

    private Map<Long, List<IngredientStockBatch>> loadBatchesGroupedByIngredient(Long storeId, List<StockTake> items) {
        List<Long> ingredientIds = items.stream()
                .map(item -> item.getIngredient().getIngredientId())
                .distinct()
                .toList();

        return ingredientStockBatchRepository.findAvailableBatchesByStoreWithLock(storeId, ingredientIds)
                .stream()
                .collect(Collectors.groupingBy(IngredientStockBatch::getIngredientId));
    }

    private void applyConfirmToItems(
            StockTakeConfirmContext ctx,
            List<StockTake> items,
            Map<Long, List<IngredientStockBatch>> batchMap
    ) {
        for (StockTake item : items) {
            Long ingredientId = item.getIngredient().getIngredientId();
            List<IngredientStockBatch> batches = batchMap.getOrDefault(ingredientId, List.of());
            confirmIndividualItem(ctx, item, batches);
        }
    }

    private void confirmIndividualItem(
            StockTakeConfirmContext ctx,
            StockTake item,
            List<IngredientStockBatch> batches
    ) {
        BigDecimal varianceQty = item.getVarianceQty();
        adjustStockByVariance(ctx, item.getIngredient(), batches, varianceQty);
    }

    private void adjustStockByVariance(
            StockTakeConfirmContext ctx,
            Ingredient ingredient,
            List<IngredientStockBatch> batches,
            BigDecimal variance
    ) {
        int compare = variance.signum();

        if (compare < 0) {
            handleStockDeficit(ctx, ingredient, batches, variance.abs());
        } else if (compare > 0) {
            createAdjustmentBatch(ctx, ingredient, variance);
        }
    }

    private void handleStockDeficit(
            StockTakeConfirmContext ctx,
            Ingredient ingredient,
            List<IngredientStockBatch> batches,
            BigDecimal deficitAmount
    ) {
        BigDecimal remainingToDeduct = deficitAmount;

        for (IngredientStockBatch batch : batches) {
            if (remainingToDeduct.signum() <= 0) {
                break;
            }

            BigDecimal currentRemaining = batch.getRemainingQuantity();
            BigDecimal deductAmount = currentRemaining.min(remainingToDeduct);
            BigDecimal after = currentRemaining.subtract(deductAmount);

            batch.updateRemaining(after);

            stockLogService.logDeduction(
                    StockDeductionLogCommand.forStockTake(
                            ctx.store(),
                            ingredient,
                            batch,
                            deductAmount,
                            after,
                            ctx.sheetId(),
                            ctx.user()
                    )
            );

            remainingToDeduct = remainingToDeduct.subtract(deductAmount);
        }

        if (remainingToDeduct.signum() > 0) {
            throw new StockException(StockErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private void createAdjustmentBatch(
            StockTakeConfirmContext ctx,
            Ingredient ingredient,
            BigDecimal amount
    ) {
        BigDecimal adjustmentUnitCost = ingredientStockBatchRepository
                .findLatestUnitCostByStoreAndIngredient(ctx.storeId(), ingredient.getIngredientId())
                .orElseGet(() -> {
                    log.warn(
                            "재고 조정 중 단가를 찾을 수 없습니다. (식재료: {}, 매장: {}). 단가 0으로 생성됩니다.",
                            ingredient.getName(),
                            ctx.storeId()
                    );
                    return BigDecimal.ZERO;
                });

        IngredientStockBatch adjustmentBatch = IngredientStockBatch.createAdjustment(
                ingredient,
                amount,
                adjustmentUnitCost,
                ingredient.getName()
        );

        ingredientStockBatchRepository.save(adjustmentBatch);

        stockLogService.logInbound(
                StockInboundLogCommand.forStockTake(
                        ctx.store(),
                        ingredient,
                        adjustmentBatch,
                        amount,
                        adjustmentBatch.getRemainingQuantity(),
                        ctx.sheetId(),
                        ctx.user()
                )
        );
    }
}