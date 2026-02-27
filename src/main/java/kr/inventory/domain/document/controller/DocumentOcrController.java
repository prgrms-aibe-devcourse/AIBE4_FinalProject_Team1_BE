package kr.inventory.domain.document.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inventory.domain.auth.security.CustomUserDetails;
import kr.inventory.domain.document.controller.dto.document.DocumentResponse;
import kr.inventory.domain.document.controller.dto.ocr.OcrResultResponse;
import kr.inventory.domain.document.service.DocumentOcrService;
import kr.inventory.domain.document.service.DocumentService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents/{storePublicId}")
@RequiredArgsConstructor
@Tag(name = "Document OCR API", description = "OCR")
public class DocumentOcrController {

	private final DocumentOcrService documentOcrService;
	private final DocumentService documentService;

	@Operation(summary = "OCR 포함 파일 처리")
	@PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<OcrResultResponse> processOcr(
		@PathVariable("storePublicId") UUID storePublicId,
		@AuthenticationPrincipal CustomUserDetails principal,
		@RequestPart("files") List<MultipartFile> files
	) {
		OcrResultResponse response = documentOcrService.processOcr(
			storePublicId,
			principal.getUserId(),
			files);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "등록된 파일목록 조회")
	@GetMapping()
	public ResponseEntity<List<DocumentResponse>> getDocuments(
		@PathVariable("storePublicId") UUID storePublicId,
		@AuthenticationPrincipal CustomUserDetails principal) {

		List<DocumentResponse> responses = documentService.getDocuments(storePublicId, principal.getUserId());
		return ResponseEntity.ok(responses);
	}
}
