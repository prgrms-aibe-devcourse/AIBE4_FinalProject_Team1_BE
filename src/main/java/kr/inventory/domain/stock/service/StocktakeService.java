package kr.inventory.domain.stock.service;

import jakarta.transaction.Transactional;
import kr.inventory.domain.catalog.entity.Ingredient;
import kr.inventory.domain.catalog.repository.IngredientRepository;
import kr.inventory.domain.stock.controller.dto.StocktakeDto;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StocktakeService {
    private final StocktakeRepository stocktakeRepository;
    private final IngredientStockBatchRepository ingredientStockBatchRepository;
    private final IngredientRepository ingredientRepository;

    @Transactional
    public List<Long> inputStocktakeList(List<StocktakeDto.ItemRequest> requests){
        List<Long> ingredientIds = requests.stream()
                .map(StocktakeDto.ItemRequest::ingredientId)
                .toList();

        List<Ingredient> ingredients = ingredientRepository.findAllById(ingredientIds);

        Map<Long, Ingredient> ingredientMap = ingredients.stream()
                .collect(Collectors.toMap(Ingredient::getIngredientId, Function.identity()));

        List<Stocktake> drafts = requests.stream()
                .map(req ->{
                    Ingredient ingredient = ingredientMap.get(req.ingredientId());
                    return Stocktake.createDraft(ingredient, req.stocktakeQty());
                })
                .toList();

        return stocktakeRepository.saveAll(drafts).stream()
                .map(Stocktake::getStocktakeId)
                .toList();
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
