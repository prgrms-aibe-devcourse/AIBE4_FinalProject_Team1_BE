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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StocktakeService {
    private final StocktakeRepository stocktakeRepository;
    private final IngredientStockBatchRepository ingredientStockBatchRepository;
    private final IngredientRepository ingredientRepository;
    private final StocktakeSheetRepository stocktakeSheetRepository;

    public Long createStocktakeSheet(StocktakeDto.CreateRequest request){
        StocktakeSheet sheet = StocktakeSheet.create(request.title());
        stocktakeSheetRepository.save(sheet);

        List<Long> ingredientIds = request.items().stream().map(StocktakeDto.ItemRequest::ingredientId).toList();
        Map<Long, Ingredient> ingredientMap = ingredientRepository.findAllById(ingredientIds).stream().collect(Collectors.toMap(Ingredient::getIngredientId, Function.identity()));

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
    public void confirmSheet(Long sheetId) {
        StocktakeSheet sheet = stocktakeSheetRepository.findById(sheetId)
                .orElseThrow(() -> new StockException(StockErrorCode.SHEET_NOT_FOUND));

        if (sheet.getStatus() == StocktakeStatus.CONFIRMED) {
            throw new StockException(StockErrorCode.ALREADY_CONFIRMED);
        }

        List<Stocktake> items = stocktakeRepository.findBySheet(sheet);

        for (Stocktake item : items) {
            confirmIndividualItem(item);
        }

        sheet.confirm();
    }

    private void confirmIndividualItem(Stocktake item) {
        Ingredient ingredient = item.getIngredient();

        List<IngredientStockBatch> batches = ingredientStockBatchRepository.findAllForAdjustmentWithLock(ingredient.getIngredientId());

        BigDecimal theoreticalQty = batches.stream()
                .map(IngredientStockBatch::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal varianceQty = item.getStocktakeQty().subtract(theoreticalQty);

        item.updateQuantities(theoreticalQty, varianceQty);

        applyRedistribution(batches, item.getStocktakeQty(), ingredient);
    }

    private void applyRedistribution(List<IngredientStockBatch> batches, BigDecimal stocktakeQty, Ingredient ingredient){
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
            createAdjustmentBatch(ingredient, remainingToDistribute);
        }
    }

    private void createAdjustmentBatch(Ingredient ingredient, BigDecimal amount) {
        BigDecimal adjustmentUnitCost = ingredientStockBatchRepository
                .findFirstByIngredientOrderByCreatedAtDesc(ingredient)
                .map(IngredientStockBatch::getUnitCost)
                .orElse(BigDecimal.ZERO);

        IngredientStockBatch adjustmentBatch = IngredientStockBatch.createAdjustment(
                ingredient,
                amount,
                adjustmentUnitCost
        );

        ingredientStockBatchRepository.save(adjustmentBatch);
    }
}
