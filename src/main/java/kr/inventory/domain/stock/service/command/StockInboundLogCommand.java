package kr.inventory.domain.stock.service.command;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.enums.ReferenceType;
import kr.inventory.domain.stock.entity.enums.TransactionType;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.user.entity.User;

import java.math.BigDecimal;

public record StockInboundLogCommand(
        Store store,
        Ingredient ingredient,
        BigDecimal quantity,
        BigDecimal balanceAfter,
        IngredientStockBatch batch,
        String productDisplayName,
        ReferenceType referenceType,
        Long referenceId,
        User user,
        TransactionType transactionType
) {
    public static StockInboundLogCommand ofInbound(
            StockInbound inbound,
            StockInboundItem item,
            IngredientStockBatch batch,
            BigDecimal balanceAfter,
            User user
    ) {
        return new StockInboundLogCommand(
                inbound.getStore(),
                item.getIngredient(),
                item.getEffectiveQuantity(),
                balanceAfter,
                batch,
                batch.getProductDisplayName(),
                ReferenceType.INBOUND,
                inbound.getInboundId(),
                user,
                TransactionType.INBOUND
        );
    }

    public static StockInboundLogCommand forStockTake(
            Store store,
            Ingredient ingredient,
            IngredientStockBatch batch,
            BigDecimal quantity,
            BigDecimal balanceAfter,
            Long sheetId,
            User user
    ) {
        return new StockInboundLogCommand(
                store,
                ingredient,
                quantity,
                balanceAfter,
                batch,
                batch.getProductDisplayName(),
                ReferenceType.STOCK_TAKING,
                sheetId,
                user,
                TransactionType.ADJUST
        );
    }
}