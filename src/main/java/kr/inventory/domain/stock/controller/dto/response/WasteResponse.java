package kr.inventory.domain.stock.controller.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import kr.inventory.domain.stock.entity.WasteRecord;
import kr.inventory.domain.stock.entity.enums.WasteReason;

public record WasteResponse(
	UUID wastePublicId,
	String ingredientName,
	BigDecimal quantity,
	WasteReason reason,
	BigDecimal amount,
	OffsetDateTime wasteAt,
	String recordedBy
) {
	public static WasteResponse from(WasteRecord record) {
		return new WasteResponse(
			record.getWastePublicId(),
			record.getStockBatch().getInboundItem().getProductDisplayName(),
			record.getWasteQuantity(),
			record.getWasteReason(),
			record.getWasteAmount(),
			record.getWasteDate(),
			record.getRecordedByUser().getName()
		);
	}
}
