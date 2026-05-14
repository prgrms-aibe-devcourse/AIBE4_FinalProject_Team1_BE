package kr.inventory.global.llm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private String provider = "gemini";
    private String model = "gemini-2.5-flash";
    private String flashModel = "gemini-2.5-flash";
    private String reasoningModel = "gemini-2.5-pro";
    private String reviewModel = "gemini-2.5-flash";
    private Double flashTemperature = 0.2;
    private Double reasoningTemperature = 0.1;
    private Double reviewTemperature = 0.1;
    private Integer flashThinkingBudget = 0;
    private Integer reasoningThinkingBudget = 2048;
    private Integer flashMaxOutputTokens = 4096;
    private Integer reasoningMaxOutputTokens = 8192;
    private Integer reviewMaxOutputTokens = 4096;
    private boolean reviewEnabled = true;
    private int reviewMinAnswerLength = 220;
    private boolean rateLimitEnabled = false;
    private long minRequestIntervalMillis = 0L;
    private int quotaRetryMaxAttempts = 1;
    private long quotaRetryMaxDelayMillis = 60_000L;

    private Gemini gemini = new Gemini();

    @Getter
    @Setter
    public static class Gemini {
        private String apiKey;
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    }
}
