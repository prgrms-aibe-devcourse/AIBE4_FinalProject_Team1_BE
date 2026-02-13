package kr.inventory.domain.stock.service;

import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.catalog.exception.IngredientErrorCode;
import kr.inventory.domain.catalog.exception.IngredientException;
import kr.inventory.domain.catalog.repository.IngredientRepository;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.Stocktake;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StocktakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StocktakeService {
    private final StocktakeRepository stocktakeRepository;
    private final IngredientStockBatchRepository ingredientStockBatchRepository;
    private final IngredientRepository ingredientRepository;

    public Long inputStocktake(Long ingredientId, BigDecimal stocktakeQty){
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IngredientException(IngredientErrorCode.INGREDIENT_NOT_FOUND));

        Stocktake draft = Stocktake.createDraft(ingredient, stocktakeQty);
        return stocktakeRepository.save(draft).getStocktakeId();
    }

    public void confirmStocktake(Long stocktakeId){
        Stocktake stocktake = stocktakeRepository.findById(stocktakeId)
                .orElseThrow(() -> new StockException(StockErrorCode.DRAFT_STOCK_TAKE_NOT_FOUND));

        Ingredient ingredient = stocktake.getIngredient();

        List<IngredientStockBatch> batches = ingredientStockBatchRepository.findAllForAdjustmentWithLock(ingredient.getIngredientId());

        BigDecimal theoreticalQty = batches.stream()
                .map(IngredientStockBatch::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal varianceQty = stocktake.getStocktakeQty().subtract(theoreticalQty);

        stocktake.confirm(theoreticalQty, varianceQty);

        applyRedistribution(batches, stocktake.getStocktakeQty(), ingredient);
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
