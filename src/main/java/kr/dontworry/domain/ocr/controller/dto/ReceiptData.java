package kr.dontworry.domain.ocr.controller.dto;

public record ReceiptData(
        String storeName,
        String date,
        String amount,
        String paymentMethod,
        String category
) {
}
