package kr.inventory.domain.stock.service.command;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.store.entity.Store;

import java.math.BigDecimal;

public record StockDeductionLogCommand(
        Store store,
        Ingredient ingredient,
        BigDecimal quantity,
        BigDecimal balanceAfter,
        IngredientStockBatch batch,
        Long sourceId
) {
    public static StockDeductionLogCommand forSale(
            IngredientStockBatch batch,
            BigDecimal deductionQty,
            BigDecimal balanceAfter,
            Long salesOrderId
    ) {
        return new StockDeductionLogCommand(
                batch.getIngredient().getStore(),
                batch.getIngredient(),
                deductionQty,
                balanceAfter,
                batch,
                salesOrderId
        );
    }
}