package kr.inventory.domain.stock.service.command;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
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
	Long sourceId,
	User user,
	TransactionType type // 입고, 출고, 조정 등 구분
) {
	public static StockInboundLogCommand ofInbound(StockInbound inbound, StockInboundItem item, IngredientStockBatch batch,
                                                   BigDecimal balanceAfter, User user) {
		return new StockInboundLogCommand(
			inbound.getStore(),
			item.getIngredient(),
			item.getQuantity(),
			balanceAfter,
			batch,
			inbound.getInboundId(),
			user,
			TransactionType.INBOUND
		);
	}
}