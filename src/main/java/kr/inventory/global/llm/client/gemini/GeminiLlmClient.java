package kr.inventory.global.llm.client.gemini;

import java.util.List;
import kr.inventory.global.llm.client.LlmClient;
import kr.inventory.global.llm.config.LlmProperties;
import kr.inventory.global.llm.dto.LlmChatRequest;
import kr.inventory.global.llm.dto.LlmChatResponse;
import kr.inventory.global.llm.dto.LlmMessage;
import kr.inventory.global.llm.dto.gemini.GeminiGenerateContentRequest;
import kr.inventory.global.llm.dto.gemini.GeminiGenerateContentResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class GeminiLlmClient implements LlmClient {

    private final LlmProperties properties;
    private final RestClient restClient;

    public GeminiLlmClient(LlmProperties properties) {
        this.properties = properties;

        if (!StringUtils.hasText(properties.getGemini().getApiKey())) {
            throw new IllegalStateException("llm.gemini.api-key is not configured");
        }

        if (!StringUtils.hasText(properties.getGemini().getBaseUrl())) {
            throw new IllegalStateException("llm.gemini.base-url is not configured");
        }

        this.restClient = RestClient.builder()
                .baseUrl(properties.getGemini().getBaseUrl())
                .defaultHeader("x-goog-api-key", properties.getGemini().getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        String prompt = buildPrompt(request);

        GeminiGenerateContentRequest body = new GeminiGenerateContentRequest(
                List.of(
                        new GeminiGenerateContentRequest.Content(
                                List.of(new GeminiGenerateContentRequest.Part(prompt))
                        )
                )
        );

        GeminiGenerateContentResponse response = restClient.post()
                .uri("/models/{model}:generateContent", properties.getModel())
                .body(body)
                .retrieve()
                .body(GeminiGenerateContentResponse.class);

        String text = extractText(response);

        return new LlmChatResponse(text, properties.getModel());
    }

    private String buildPrompt(LlmChatRequest request) {
        StringBuilder sb = new StringBuilder();

        if (StringUtils.hasText(request.systemPrompt())) {
            sb.append("[SYSTEM]\n")
                    .append(request.systemPrompt())
                    .append("\n\n");
        }

        if (request.messages() != null) {
            for (LlmMessage message : request.messages()) {
                if (message == null || !StringUtils.hasText(message.content())) {
                    continue;
                }

                String role = StringUtils.hasText(message.role())
                        ? message.role().trim().toUpperCase()
                        : "USER";

                sb.append("[")
                        .append(role)
                        .append("]\n")
                        .append(message.content().trim())
                        .append("\n\n");
            }
        }

        sb.append("[ASSISTANT]\n");

        return sb.toString();
    }

    private String extractText(GeminiGenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Gemini response is empty");
        }

        GeminiGenerateContentResponse.Candidate candidate = response.candidates().get(0);

        if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
            throw new IllegalStateException("Gemini response has no text parts");
        }

        StringBuilder sb = new StringBuilder();
        for (GeminiGenerateContentResponse.Part part : candidate.content().parts()) {
            if (part.text() != null) {
                sb.append(part.text());
            }
        }

        String text = sb.toString().trim();
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("Gemini returned blank text");
        }

        return text;
    }
}