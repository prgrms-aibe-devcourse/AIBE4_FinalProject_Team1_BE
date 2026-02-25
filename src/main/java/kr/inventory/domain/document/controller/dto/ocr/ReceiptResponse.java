package kr.inventory.domain.document.controller.dto.ocr;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import kr.inventory.domain.document.service.processor.OcrValidator;

public record ReceiptResponse(
	Field<String> vendorName,
	Field<String> date,
	Field<String> amount,
	List<Item> items
) {

	public static ReceiptResponse of(
		Field<String> vendorName,
		Field<String> date,
		Field<String> amount,
		List<Item> items
	) {
		return new ReceiptResponse(
			Objects.requireNonNull(vendorName),
			Objects.requireNonNull(date),
			Objects.requireNonNull(amount),
			items == null ? Collections.emptyList() : List.copyOf(items)
		);
	}

	public static ReceiptResponse fromRaw(RawReceiptData raw) {

		List<Item> validatedItems = raw.items().stream()
			.map(Item::fromRaw)
			.toList();

		return of(
			OcrValidator.validate(raw.vendorName(), "공급처"),
			OcrValidator.validate(raw.date(), "날짜"),
			OcrValidator.validate(raw.amount(), "총액"),
			validatedItems
		);
	}

	public static ReceiptResponse empty(String msg) {
		Field<String> errorField = Field.fail(msg);
		return of(errorField, errorField, errorField, Collections.emptyList());
	}

	public record Item(
		Field<String> name,
		Field<String> quantity,
		Field<String> rawCapacity,
		Field<String> costPrice,
		Field<String> totalPrice,
		Field<String> expirationDate
	) {

		public static Item of(
			Field<String> name,
			Field<String> quantity,
			Field<String> rawCapacity,
			Field<String> costPrice,
			Field<String> totalPrice,
			Field<String> expirationDate
		) {
			return new Item(
				Objects.requireNonNull(name),
				Objects.requireNonNull(quantity),
				rawCapacity,
				costPrice,
				totalPrice,
				expirationDate
			);
		}

		public static Item fromRaw(RawReceiptData.RawItem item) {

			Field<String> name =
				OcrValidator.validate(item.name(), "상품명");

			Field<String> qty =
				OcrValidator.validate(item.quantity(), "수량");

			Field<String> cost =
				OcrValidator.validate(item.costPrice(), "단가");

			Field<String> rawTotal =
				OcrValidator.validate(item.totalPrice(), "총액");

			Field<String> validatedTotal =
				OcrValidator.validateTotal(
					qty.value(),
					cost.value(),
					rawTotal.value()
				);

			return of(
				name,
				qty,
				OcrValidator.validateCapacity(item.rawCapacity()),
				cost,
				validatedTotal,
				OcrValidator.validate(item.expirationDate(), "유통기한")
			);
		}
	}

	public record Field<T>(T value, Status status, String message) {

		public enum Status {
			GREEN,
			YELLOW,
			RED
		}

		public static <T> Field<T> success(T value) {
			return new Field<>(value, Status.GREEN, null);
		}

		public static <T> Field<T> warning(T value, String msg) {
			return new Field<>(value, Status.YELLOW, msg);
		}

		public static <T> Field<T> fail(String msg) {
			return new Field<>(null, Status.RED, msg);
		}
	}
}