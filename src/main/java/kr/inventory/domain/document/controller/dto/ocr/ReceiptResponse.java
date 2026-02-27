package kr.inventory.domain.document.controller.dto.ocr;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record ReceiptResponse(
	VendorField vendor,
	Field<String> date,
	Field<String> amount,
	List<Item> items
) {

	public static ReceiptResponse of(
		VendorField vendor,
		Field<String> date,
		Field<String> amount,
		List<Item> items
	) {
		return new ReceiptResponse(
			Objects.requireNonNull(vendor),
			Objects.requireNonNull(date),
			Objects.requireNonNull(amount),
			items == null ? Collections.emptyList() : List.copyOf(items)
		);
	}

	public static ReceiptResponse empty(String msg) {
		Field<String> errorField = Field.fail(msg);
		return of(
			new VendorField(Field.fail(msg), Field.fail(msg)),
			errorField,
			errorField,
			Collections.emptyList()
		);
	}

	public record VendorField(
		Field<Long> id,
		Field<String> name
	) {
	}

	public record IngredientField(
		Field<Long> id,
		Field<String> name
	) {
	}

	public record Item(
		IngredientField ingredient,
		Field<String> quantity,
		Field<String> rawCapacity,
		Field<String> costPrice,
		Field<String> totalPrice,
		Field<String> expirationDate
	) {
		public static Item of(
			IngredientField ingredient,
			Field<String> quantity,
			Field<String> rawCapacity,
			Field<String> costPrice,
			Field<String> totalPrice,
			Field<String> expirationDate
		) {
			return new Item(
				Objects.requireNonNull(ingredient),
				Objects.requireNonNull(quantity),
				rawCapacity,
				costPrice,
				totalPrice,
				expirationDate
			);
		}
	}

	public record Field<T>(
		T value,
		Status status,
		String message
	) {
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