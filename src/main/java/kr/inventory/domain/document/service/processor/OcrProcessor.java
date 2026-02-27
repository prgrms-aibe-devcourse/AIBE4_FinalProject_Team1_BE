package kr.inventory.domain.document.service.processor;

import kr.inventory.domain.document.controller.dto.ocr.ReceiptResponse;

import org.springframework.web.multipart.MultipartFile;

public interface OcrProcessor {
	boolean supports(MultipartFile file);

	ReceiptResponse process(MultipartFile file, Long storeId);
}
