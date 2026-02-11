package kr.inventory.domain.document.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.document.controller.dto.ocr.OcrResultResponse;
import kr.inventory.domain.document.service.DocumentOcrService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents/ocr")
@RequiredArgsConstructor
@Tag(name = "Document OCR API", description = "OCR")
public class DocumentOcrController {

	private final DocumentOcrService documentOcrService;

	@Operation(summary = "OCR 포함 파일 처리")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<OcrResultResponse> processOcr(
		@RequestParam("storeId") Long storeId,
		@AuthenticationPrincipal CustomUserDetails principal,
		@RequestPart("files") List<MultipartFile> files
	) {
		OcrResultResponse response = documentOcrService.processOcr(
			storeId,
			principal.getUserId(),
			files);
		return ResponseEntity.ok(response);
	}
}
