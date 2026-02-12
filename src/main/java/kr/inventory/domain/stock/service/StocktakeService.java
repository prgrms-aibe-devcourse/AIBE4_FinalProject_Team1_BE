package kr.inventory.domain.stock.service;

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

    public Long inputStocktake(Long ingredientId, BigDecimal stocktakeQty){
        Stocktake draft = Stocktake.createDraft(ingredientId, stocktakeQty);
        return stocktakeRepository.save(draft).getStocktakeId();
    }

    public void confirmStocktake(Long stocktakeId){
        Stocktake stocktake = stocktakeRepository.findById(stocktakeId)
                .orElseThrow(() -> new StockException(StockErrorCode.DRAFT_STOCK_TAKE_NOT_FOUND));

        List<IngredientStockBatch> batches = ingredientStockBatchRepository.findAllForAdjustmentWithLock(stocktake.getIngredientId());

        BigDecimal theoreticalQty = batches.stream()
                .map(IngredientStockBatch::getRemainingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal varianceQty = stocktake.getStocktakeQty().subtract(theoreticalQty);

        stocktake.confirm(theoreticalQty, varianceQty);

        applyRedistribution(batches, stocktake.getStocktakeQty(), stocktake.getIngredientId());
    }

    private void applyRedistribution(List<IngredientStockBatch> batches, BigDecimal stocktakeQty, Long ingredientId){
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
            createAdjustmentBatch(ingredientId, remainingToDistribute);
        }
    }

    private void createAdjustmentBatch(Long ingredientId, BigDecimal amount) {
        // TODO: 기존 배치를 넘어서 배치가 생성되어야 하는 경우 레코드 추가..
    }
}
