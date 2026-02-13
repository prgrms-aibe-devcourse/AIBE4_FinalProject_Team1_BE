package kr.inventory.domain.document.controller.dto.ocr;

import java.util.List;

public record RawReceiptData(
	String vendorName,
	String date,
	String amount,
	List<RawItem> items
) {
	public record RawItem(
		String name,
		String quantity,
		String costPrice,
		String totalPrice,
		String expirationDate,
		String rawCapacity
	) {
	}
}