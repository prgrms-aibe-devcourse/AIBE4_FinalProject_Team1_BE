package kr.inventory.domain.document.service.processor;

import java.math.BigDecimal;

import kr.inventory.domain.document.controller.dto.ocr.ReceiptData;
import kr.inventory.global.util.UnitConverter;

public class OcrValidator {

	public static ReceiptData.Field<String> validate(String value, String fieldName) {
		if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
			return ReceiptData.Field.fail(fieldName + " 정보 누락");
		}
		return ReceiptData.Field.success(value);
	}

	public static ReceiptData.Field<String> validateCapacity(String rawCapacity) {
		if (rawCapacity == null || rawCapacity.isBlank() || "null".equalsIgnoreCase(rawCapacity)) {
			return ReceiptData.Field.warning(null, "용량 정보 없음");
		}

		String normalized = UnitConverter.convertToStandardUnit(rawCapacity);

		if (normalized == null || normalized.isEmpty()) {
			return ReceiptData.Field.warning(rawCapacity, "단위 변환 실패");
		}

		return ReceiptData.Field.success(normalized);
	}

	public static ReceiptData.Field<String> validateTotal(String qtyStr, String costStr, String totalStr) {
		if (qtyStr == null || costStr == null || totalStr == null) {
			return ReceiptData.Field.warning(totalStr, "계산에 필요한 정보 부족");
		}

		try {
			BigDecimal qty = new BigDecimal(qtyStr.replaceAll("[^0-9.]", ""));
			BigDecimal cost = new BigDecimal(costStr.replaceAll("[^0-9.]", ""));
			BigDecimal total = new BigDecimal(totalStr.replaceAll("[^0-9.]", ""));

			if (qty.multiply(cost).compareTo(total) == 0) {
				return ReceiptData.Field.success(totalStr);
			}
			return ReceiptData.Field.warning(totalStr, "계산 불일치 (수량x단가 != 총액)");
		} catch (Exception e) {
			return ReceiptData.Field.warning(totalStr, "금액 형식 오류");
		}
	}
}
