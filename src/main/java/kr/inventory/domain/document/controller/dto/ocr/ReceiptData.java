package kr.inventory.domain.document.controller.dto.ocr;

import java.util.Collections;
import java.util.List;

public record ReceiptData(
	Field<String> vendorName,
	Field<String> date,
	Field<String> amount,
	List<Item> items
) {
	public record Item(
		Field<String> name,
		Field<String> quantity,
		Field<String> rawCapacity,
		Field<String> costPrice,
		Field<String> totalPrice,
		Field<String> expirationDate
	) {
	}

	public record Field<T>(T value, String status, String message) {
		public static <T> Field<T> success(T value) {
			return new Field<>(value, "GREEN", null);
		}

		public static <T> Field<T> warning(T value, String msg) {
			return new Field<>(value, "YELLOW", msg);
		}

		public static <T> Field<T> fail(String msg) {
			return new Field<>(null, "RED", msg);
		}
	}

	public static ReceiptData empty(String msg) {
		Field<String> errorField = Field.fail(msg);
		return new ReceiptData(errorField, errorField, errorField, Collections.emptyList());
	}
}
