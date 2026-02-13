package kr.inventory.domain.document.controller.dto.ocr;

import java.util.List;

public record OcrResultResponse(
	List<ReceiptResponse> results
) {
}
