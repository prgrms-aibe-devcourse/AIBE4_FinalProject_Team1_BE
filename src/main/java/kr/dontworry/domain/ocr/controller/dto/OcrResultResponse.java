package kr.dontworry.domain.ocr.controller.dto;

import java.util.List;

public record OcrResultResponse(
        List<ReceiptData> results
) {
}
