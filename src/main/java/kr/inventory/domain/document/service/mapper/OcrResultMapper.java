package kr.inventory.domain.document.service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.document.controller.dto.ocr.RawReceiptData;
import kr.inventory.domain.document.controller.dto.ocr.ReceiptResponse;
import kr.inventory.domain.document.service.processor.OcrValidator;
import kr.inventory.domain.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OcrResultMapper {

	private final VendorRepository vendorRepository;
	private final IngredientRepository ingredientRepository;

	public ReceiptResponse mapToReceiptResponse(RawReceiptData raw, Long storeId) {

		ReceiptResponse.Field<Long> matchedVendorId = OcrValidator.validateVendor(
			raw.vendorName(),
			storeId,
			vendorRepository
		);

		List<ReceiptResponse.Item> validatedItems = raw.items().stream()
			.map(item -> mapToItem(item, storeId))
			.toList();

		return ReceiptResponse.of(
			new ReceiptResponse.VendorField(
				matchedVendorId,
				OcrValidator.validate(raw.vendorName(), "공급처")
			),
			OcrValidator.validate(raw.date(), "날짜"),
			OcrValidator.validate(raw.amount(), "총액"),
			validatedItems
		);
	}

	private ReceiptResponse.Item mapToItem(RawReceiptData.RawItem item, Long storeId) {
		ReceiptResponse.Field<Long> emptyId = ReceiptResponse.Field.success(null);
		ReceiptResponse.Field<String> rawName = OcrValidator.validate(item.name(), "상품명");

		return ReceiptResponse.Item.of(
			new ReceiptResponse.IngredientField(emptyId, rawName),
			OcrValidator.validate(item.quantity(), "수량"),
			OcrValidator.validateCapacity(item.rawCapacity()),
			OcrValidator.validate(item.costPrice(), "단가"),
			OcrValidator.validateTotal(item.quantity(), item.costPrice(), item.totalPrice()),
			OcrValidator.validate(item.expirationDate(), "유통기한")
		);
	}
}
