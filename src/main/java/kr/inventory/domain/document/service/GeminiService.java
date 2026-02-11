package kr.inventory.domain.document.service;

import kr.inventory.domain.document.controller.dto.gemini.GeminiRequest;
import kr.inventory.domain.document.controller.dto.gemini.GeminiResponse;
import kr.inventory.domain.document.exception.OcrError;
import kr.inventory.domain.document.exception.OcrException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

	@Value("${gemini.api.key}")
	private String geminiApiKey;

	@Value("${gemini.api.url}")
	private String geminiApiUrl;

	private final RestClient restClient = RestClient.create();

	public String callGeminiApi(byte[] fileData, String mimeType, String prompt) {
		String base64Data = Base64.getEncoder().encodeToString(fileData);

		GeminiRequest request = new GeminiRequest(
			List.of(new GeminiRequest.Content(
				List.of(
					new GeminiRequest.TextPart(prompt),
					new GeminiRequest.InlineDataPart(
						new GeminiRequest.InlineData(mimeType, base64Data)
					)
				)
			))
		);

		try {
			GeminiResponse response = restClient.post()
				.uri(geminiApiUrl + "?key=" + geminiApiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(GeminiResponse.class);

			return extractTextFromResponse(response);
		} catch (Exception e) {
			log.error("Gemini API call failed", e);
			throw new OcrException(OcrError.GEMINI_API_ERROR);
		}
	}

	private String extractTextFromResponse(GeminiResponse response) {
		if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
			return response.candidates().get(0).content().parts().get(0).text();
		}
		throw new OcrException(OcrError.NO_RESPONSE_FROM_GEMINI);
	}
}