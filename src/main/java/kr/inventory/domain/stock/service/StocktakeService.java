package kr.inventory.domain.stock.service;

import jakarta.transaction.Transactional;
import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.catalog.exception.IngredientErrorCode;
import kr.inventory.domain.catalog.exception.IngredientException;
import kr.inventory.domain.catalog.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.StocktakeDto;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.Stocktake;
import kr.inventory.domain.stock.entity.StocktakeSheet;
import kr.inventory.domain.stock.entity.enums.StocktakeStatus;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StocktakeRepository;
import kr.inventory.domain.stock.repository.StocktakeSheetRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StocktakeService {
    private final StocktakeRepository stocktakeRepository;
    private final IngredientStockBatchRepository ingredientStockBatchRepository;
    private final IngredientRepository ingredientRepository;
    private final StocktakeSheetRepository stocktakeSheetRepository;
    private final StoreAccessValidator storeAccessValidator;

    @Transactional
    public Long createStocktakeSheet(Long userId, UUID storePublicId, StocktakeDto.CreateRequest request){
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StocktakeSheet sheet = StocktakeSheet.create(storeId, request.title());
        stocktakeSheetRepository.save(sheet);

        List<Long> ingredientIds = request.items().stream().map(StocktakeDto.ItemRequest::ingredientId).toList();

        Map<Long, Ingredient> ingredientMap = ingredientRepository.findAllByStoreIdAndIdIn(storeId, ingredientIds).stream().collect(Collectors.toMap(Ingredient::getIngredientId, Function.identity()));

        List<Stocktake> items = request.items().stream()
                .map(req -> {
                    Ingredient ingredient = Optional.ofNullable(ingredientMap.get(req.ingredientId()))
                            .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));
                    return Stocktake.createDraft(sheet, ingredient, req.stocktakeQty());
                })
                .toList();

        stocktakeRepository.saveAll(items);
        return sheet.getSheetId();
    }

    @Transactional
    public void confirmSheet(Long userId, UUID storePublicId, Long sheetId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        StocktakeSheet sheet = getValidSheetForConfirm(sheetId, storeId);

        List<Stocktake> items = stocktakeRepository.findBySheet(sheet);
        if (items.isEmpty()) return;

        Map<Long, List<IngredientStockBatch>> batchMap = fetchGroupedBatches(storeId, items);

        processStocktakeItems(storeId, items, batchMap);

        sheet.confirm();
    }

    private StocktakeSheet getValidSheetForConfirm(Long sheetId, Long storeId) {
        StocktakeSheet sheet = stocktakeSheetRepository.findByIdAndStoreId(sheetId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.SHEET_NOT_FOUND));

        if (sheet.getStatus() == StocktakeStatus.CONFIRMED) {
            throw new StockException(StockErrorCode.ALREADY_CONFIRMED);
        }
        return sheet;
    }

    private Map<Long, List<IngredientStockBatch>> fetchGroupedBatches(Long storeId, List<Stocktake> items) {
        List<Long> ingredientIds = items.stream()
                .map(item -> item.getIngredient().getIngredientId())
                .toList();

        return ingredientStockBatchRepository.findAvailableBatchesByStoreWithLock(storeId, ingredientIds)
                .stream()
                .collect(Collectors.groupingBy(IngredientStockBatch::getIngredientId));
    }

    private void processStocktakeItems(Long storeId, List<Stocktake> items, Map<Long, List<IngredientStockBatch>> batchMap) {
        for (Stocktake item : items) {
            Long ingredientId = item.getIngredient().getIngredientId();
            List<IngredientStockBatch> ingredientBatches = batchMap.getOrDefault(ingredientId, List.of());

            confirmIndividualItem(storeId, item, ingredientBatches);
        }
    }

    private void confirmIndividualItem(Long storeId, Stocktake item, List<IngredientStockBatch> batches){
        Ingredient ingredient = item.getIngredient();

        BigDecimal theoreticalQty = batches.stream()
                .map(IngredientStockBatch::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal varianceQty = item.getStocktakeQty().subtract(theoreticalQty);

        item.updateQuantities(theoreticalQty, varianceQty);

        applyRedistribution(storeId, batches, item.getStocktakeQty(), ingredient);
    }

    private void applyRedistribution(Long storeId, List<IngredientStockBatch> batches, BigDecimal stocktakeQty, Ingredient ingredient){
        BigDecimal remainingToDistribute = stocktakeQty;

        for(IngredientStockBatch batch : batches){
            if(remainingToDistribute.signum() <= 0){
                batch.updateRemaining(BigDecimal.ZERO);
            } else{
                BigDecimal fillAmount = remainingToDistribute.min(batch.getInitialQuantity());
                batch.updateRemaining(fillAmount);

                remainingToDistribute = remainingToDistribute.subtract(fillAmount);
            }
        }

        if(remainingToDistribute.signum() > 0){
            createAdjustmentBatch(storeId, ingredient, remainingToDistribute);
        }
    }

    private void createAdjustmentBatch(Long storeId, Ingredient ingredient, BigDecimal amount) {
        BigDecimal adjustmentUnitCost = ingredientStockBatchRepository
                .findLatestUnitCostByStoreAndIngredient(storeId, ingredient.getIngredientId())
                .orElse(BigDecimal.ZERO);

        IngredientStockBatch adjustmentBatch = IngredientStockBatch.createAdjustment(
                storeId,
                ingredient,
                amount,
                adjustmentUnitCost
        );

        ingredientStockBatchRepository.save(adjustmentBatch);
    }
}
