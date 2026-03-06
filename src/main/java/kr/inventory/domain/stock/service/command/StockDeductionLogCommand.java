package kr.inventory.domain.stock.service.command;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.enums.ReferenceType;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.user.entity.User;

import java.math.BigDecimal;

public record StockDeductionLogCommand(
        Store store,
        Ingredient ingredient,
        BigDecimal quantity,
        BigDecimal balanceAfter,
        IngredientStockBatch batch,
        ReferenceType referenceType,
        Long referenceId,
        User createdByUser
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
                ReferenceType.SALE,
                salesOrderId,
                null
        );
    }

    public static StockDeductionLogCommand forStockTake(
            Store store,
            Ingredient ingredient,
            IngredientStockBatch batch,
            BigDecimal deductionQty,
            BigDecimal balanceAfter,
            Long sheetId,
            User user
    ) {
        return new StockDeductionLogCommand(
                store,
                ingredient,
                deductionQty,
                balanceAfter,
                batch,
                ReferenceType.STOCK_TAKING,
                sheetId,
                user
        );
    }
}