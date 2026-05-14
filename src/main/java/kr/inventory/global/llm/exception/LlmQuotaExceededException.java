package kr.inventory.global.llm.exception;

import lombok.Getter;

@Getter
public class LlmQuotaExceededException extends RuntimeException {

    private final long retryAfterMillis;
    private final String model;

    public LlmQuotaExceededException(String message, long retryAfterMillis, String model) {
        super(message);
        this.retryAfterMillis = Math.max(0L, retryAfterMillis);
        this.model = model;
    }

    public LlmQuotaExceededException(String message, long retryAfterMillis, String model, Throwable cause) {
        super(message, cause);
        this.retryAfterMillis = Math.max(0L, retryAfterMillis);
        this.model = model;
    }
}
