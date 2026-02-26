package kr.inventory.domain.document.service.processor;

import java.math.BigDecimal;

import kr.inventory.domain.document.controller.dto.ocr.ReceiptResponse;
import kr.inventory.global.util.UnitConverter;

public class OcrValidator {

	public static ReceiptResponse.Field<String> validate(String value, String fieldName) {
		if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
			return ReceiptResponse.Field.fail(fieldName + " 정보 누락");
		}
		return ReceiptResponse.Field.success(value);
	}

	public static ReceiptResponse.Field<String> validateCapacity(String rawCapacity) {
		if (rawCapacity == null || rawCapacity.isBlank() || "null".equalsIgnoreCase(rawCapacity)) {
			return ReceiptResponse.Field.warning(null, "용량 정보 없음");
		}

		String normalized = UnitConverter.convertToStandardUnit(rawCapacity);

		if (normalized == null || normalized.isEmpty()) {
			return ReceiptResponse.Field.warning(rawCapacity, "단위 변환 실패");
		}

		return ReceiptResponse.Field.success(normalized);
	}

	public static ReceiptResponse.Field<String> validateTotal(String qtyStr, String costStr, String totalStr) {
		if (qtyStr == null || costStr == null || totalStr == null) {
			return ReceiptResponse.Field.warning(totalStr, "계산에 필요한 정보 부족");
		}

		try {
			BigDecimal qty = new BigDecimal(qtyStr.replaceAll("[^0-9.]", ""));
			BigDecimal cost = new BigDecimal(costStr.replaceAll("[^0-9.]", ""));
			BigDecimal total = new BigDecimal(totalStr.replaceAll("[^0-9.]", ""));

			if (qty.multiply(cost).compareTo(total) == 0) {
				return ReceiptResponse.Field.success(totalStr);
			}
			return ReceiptResponse.Field.warning(totalStr, "계산 불일치 (수량x단가 != 총액)");
		} catch (Exception e) {
			return ReceiptResponse.Field.warning(totalStr, "금액 형식 오류");
		}
	}
}
