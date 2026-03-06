package kr.inventory.domain.stock.service.command;

import java.math.BigDecimal;
import java.util.UUID;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.user.entity.User;

public record StockWasteCommand(
	Store store,
	Ingredient ingredient,
	IngredientStockBatch batch,
	BigDecimal quantity,
	BigDecimal balanceAfter,
	Long sourceId,
	User user
) {
}
