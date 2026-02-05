package kr.dontworry.domain.ocr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.dontworry.domain.ocr.controller.dto.OcrResultResponse;
import kr.dontworry.domain.ocr.controller.dto.ReceiptData;
import kr.dontworry.domain.ocr.controller.dto.gemini.GeminiRequest;
import kr.dontworry.domain.ocr.controller.dto.gemini.GeminiResponse;
import kr.dontworry.domain.ocr.exception.OcrError;
import kr.dontworry.domain.ocr.exception.OcrException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    private static final String PROMPT = """
            Analyze this receipt image and extract the following information in JSON format:
            1. storeName: The name of the store or merchant.
            2. date: The transaction date (YYYY-MM-DD format).
            3. amount: The total amount paid (remove currency symbols, just numbers).
            4. paymentMethod: "CASH" or "CARD". If not found, guess based on context or return "UNKNOWN".
            
            Return ONLY the JSON object, no markdown formatting or other text.
            Example:
            {
              "storeName": "Starbucks",
              "date": "2023-10-25",
              "amount": "15000",
              "paymentMethod": "CARD"
            }
            """;

    public OcrResultResponse processOcr(List<MultipartFile> files) {
        List<ReceiptData> results = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                String jsonResponse = callGeminiApi(file);
                String cleanJson = cleanJsonString(jsonResponse);
                
                ReceiptData data = objectMapper.readValue(cleanJson, ReceiptData.class);
                results.add(data);
            } catch (OcrException e) {
                log.error("OCR Exception for file {}: {}", file.getOriginalFilename(), e.getErrorModel().getMessage());
                results.add(new ReceiptData("Error", "", "0", "UNKNOWN"));
            } catch (Exception e) {
                log.error("Unexpected error for file {}: {}", file.getOriginalFilename(), e.getMessage());
                results.add(new ReceiptData("Error", "", "0", "UNKNOWN"));
            }
        }

        return new OcrResultResponse(results);
    }

    private String callGeminiApi(MultipartFile file) {
        String base64Image;
        try {
            base64Image = Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new OcrException(OcrError.FILE_PROCESSING_ERROR);
        }
        
        String mimeType = file.getContentType();

        GeminiRequest request = new GeminiRequest(
                List.of(new GeminiRequest.Content(
                        List.of(
                                new GeminiRequest.TextPart(PROMPT),
                                new GeminiRequest.InlineDataPart(
                                        new GeminiRequest.InlineData(mimeType, base64Image)
                                )
                        )
                ))
        );

        GeminiResponse response;
        try {
            response = restClient.post()
                    .uri(geminiApiUrl + "?key=" + geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            throw new OcrException(OcrError.GEMINI_API_ERROR);
        }

        if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
            GeminiResponse.Candidate candidate = response.candidates().get(0);
            if (candidate.content() != null && candidate.content().parts() != null && !candidate.content().parts().isEmpty()) {
                return candidate.content().parts().get(0).text();
            }
        }
        
        throw new OcrException(OcrError.NO_RESPONSE_FROM_GEMINI);
    }

    private String cleanJsonString(String json) {
        if (json == null) return "{}";
        String cleaned = json.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}
