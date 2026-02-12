package kr.inventory.domain.document.service.processor;

import kr.inventory.domain.document.controller.dto.ocr.ReceiptData;
import org.springframework.web.multipart.MultipartFile;

public interface OcrProcessor {
    boolean supports(MultipartFile file);
    ReceiptData process(MultipartFile file);
}
