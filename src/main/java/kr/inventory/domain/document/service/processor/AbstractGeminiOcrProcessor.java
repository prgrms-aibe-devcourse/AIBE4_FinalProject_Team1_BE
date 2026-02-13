package kr.inventory.domain.document.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.inventory.domain.document.controller.dto.ocr.RawReceiptData;
import kr.inventory.domain.document.controller.dto.ocr.ReceiptData;
import kr.inventory.domain.document.exception.OcrException;
import kr.inventory.domain.document.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractGeminiOcrProcessor implements OcrProcessor {

	private final GeminiService geminiService;

	private final ObjectMapper objectMapper;

	private static final String PROMPT_TEMPLATE = """
		Return ONLY JSON. No markdown tags.
		
		 [Fields]
		 - vendorName: vendor name. no store name
		 - date: YYYY-MM-DD.
		 - amount: total grand total (number).
		 - items[]:
		   - name: product name.
		   - quantity: number only.
		   - costPrice: unit price.
		   - totalPrice: line total.
		   - expirationDate: YYYY-MM-DD or null.
		   - rawCapacity: capacity string from name (e.g. "1kg", "500ml") or null.
		
		 [Rules]
		 1. Extract ALL items. No summary.
		 2. Keep original language.
		 3. If value is unknown, return null.
		 4. Ensure strictly valid JSON.
		
		 Example:
		 {"vendorName":"대구청과","date":"2026-02-11","amount":22000,"items":[{"name":"깐마늘 2kg","quantity":1,"costPrice":22000,"totalPrice":22000,"expirationDate":null,"rawCapacity":"2kg"}]}
		""";

	@Override
	public ReceiptData process(MultipartFile file) {
		long start = System.currentTimeMillis();
		long apiStart = System.currentTimeMillis();
		log.info("Processing {} with Gemini: {}", file.getContentType(), file.getOriginalFilename());

		try {

			byte[] optimizedData = getOptimizedData(file);
			log.info("이미지 변환 소요 시간: {}ms", System.currentTimeMillis() - start);
			String contentType = getTargetContentType(file);

			String jsonResponse = geminiService.callGeminiApi(optimizedData, contentType, PROMPT_TEMPLATE);
			log.info("Gemini API 응답 소요 시간: {}ms", System.currentTimeMillis() - apiStart);

			String cleanJson = cleanJsonString(jsonResponse);

			RawReceiptData raw = objectMapper.readValue(cleanJson, RawReceiptData.class);

			return convertToValidatedReceiptData(raw);
		} catch (OcrException e) {
			log.error("OCR Exception for file {}: {}", file.getOriginalFilename(), e.getErrorModel().getMessage());
			return ReceiptData.empty("데이터를 불러오는 중 오류가 발생했습니다.");
		} catch (Exception e) {
			log.error("Unexpected error for file {}: {}", file.getOriginalFilename(), e.getMessage());
			return ReceiptData.empty("데이터를 불러오는 중 오류가 발생했습니다.");
		}
	}

	private ReceiptData convertToValidatedReceiptData(RawReceiptData raw) {
		List<ReceiptData.Item> validatedItems = raw.items().stream()
			.map(this::mapToItem)
			.toList();

		return new ReceiptData(
			OcrValidator.validate(raw.vendorName(), "공급처"),
			OcrValidator.validate(raw.date(), "날짜"),
			OcrValidator.validate(raw.amount(), "총액"),
			validatedItems
		);
	}

	private ReceiptData.Item mapToItem(RawReceiptData.RawItem item) {
		ReceiptData.Field<String> name = OcrValidator.validate(item.name(), "상품명");
		ReceiptData.Field<String> qty = OcrValidator.validate(item.quantity(), "수량");
		ReceiptData.Field<String> cost = OcrValidator.validate(item.costPrice(), "단가");
		ReceiptData.Field<String> rawTotal = OcrValidator.validate(item.totalPrice(), "총액");

		ReceiptData.Field<String> validatedTotal = OcrValidator.validateTotal(
			qty.value(),
			cost.value(),
			rawTotal.value()
		);

		return new ReceiptData.Item(
			name,
			qty,
			OcrValidator.validateCapacity(item.rawCapacity()),
			cost,
			validatedTotal,
			OcrValidator.validate(item.expirationDate(), "유통기한")
		);
	}

	private String cleanJsonString(String json) {
		return json.replaceAll("```json|```", "").trim();
	}

	protected abstract byte[] getOptimizedData(MultipartFile file) throws IOException;

	protected abstract String getTargetContentType(MultipartFile file);
}
