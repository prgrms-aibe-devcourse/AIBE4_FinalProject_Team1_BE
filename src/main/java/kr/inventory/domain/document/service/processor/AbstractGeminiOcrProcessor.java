package kr.inventory.domain.document.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.inventory.domain.document.controller.dto.ocr.RawReceiptData;
import kr.inventory.domain.document.controller.dto.ocr.ReceiptResponse;
import kr.inventory.domain.document.exception.OcrException;
import kr.inventory.domain.document.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractGeminiOcrProcessor implements OcrProcessor {

	private final GeminiService geminiService;
	private final ObjectMapper objectMapper;
	private final OcrPromptProvider ocrPromptProvider;

	@Override
	public ReceiptResponse process(MultipartFile file) {
		long start = System.currentTimeMillis();
		long apiStart = System.currentTimeMillis();
		log.info("Processing {} with Gemini: {}", file.getContentType(), file.getOriginalFilename());

		try {

			byte[] optimizedData = getOptimizedData(file);
			log.info("이미지 변환 소요 시간: {}ms", System.currentTimeMillis() - start);

			String contentType = getTargetContentType(file);

			String jsonResponse = geminiService.callGeminiApi(
				optimizedData,
				contentType,
				ocrPromptProvider.getReceiptPrompt()
			);

			log.info("Gemini API 응답 소요 시간: {}ms", System.currentTimeMillis() - apiStart);

			String cleanJson = cleanJsonString(jsonResponse);

			RawReceiptData raw = objectMapper.readValue(cleanJson, RawReceiptData.class);

			return convertToValidatedReceiptData(raw);

		} catch (OcrException e) {
			log.error("OCR Exception for file {}: {}", file.getOriginalFilename(),
				e.getErrorModel().getMessage());
			return ReceiptResponse.empty("데이터를 불러오는 중 오류가 발생했습니다.");
		} catch (Exception e) {
			log.error("Unexpected error for file {}: {}", file.getOriginalFilename(),
				e.getMessage());
			return ReceiptResponse.empty("데이터를 불러오는 중 오류가 발생했습니다.");
		}
	}

	private ReceiptResponse convertToValidatedReceiptData(RawReceiptData raw) {
		return ReceiptResponse.fromRaw(raw);
	}

	private String cleanJsonString(String json) {
		return json.replaceAll("```json|```", "").trim();
	}

	protected abstract byte[] getOptimizedData(MultipartFile file) throws IOException;

	protected abstract String getTargetContentType(MultipartFile file);
}