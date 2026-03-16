package kr.inventory.domain.analytics.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import kr.inventory.domain.analytics.document.stock.StockLogDocument;

public record StockLogAnalyticResponse(String id,
									   Long ingredientId,
									   String ingredientName,
									   String productDisplayName,

									   // 변동 정보
									   String transactionType,    // INBOUND(입고), DEDUCTION(차감), WASTE(폐기), ADJUST(조정)
									   String referenceType,      // SALE(판매), WASTE(폐기), STOCK_TAKING(재고실사) 등

									   BigDecimal changeQuantity,
									   BigDecimal balanceAfter,

									   OffsetDateTime createdAt
) {
	// Document -> Response 변환 편의 메서드
	public static StockLogAnalyticResponse from(StockLogDocument doc) {
		return new StockLogAnalyticResponse(
			doc.id(),
			doc.ingredientId(),
			doc.ingredientName(),
			doc.productDisplayName(),
			doc.transactionType(),
			doc.referenceType(),
			doc.changeQuantity(),
			doc.balanceAfter(),
			doc.createdAt()
		);
	}
}