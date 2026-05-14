package kr.inventory.domain.chat.service;

import java.util.Objects;
import java.util.Set;
import kr.inventory.domain.chat.service.context.ChatAnswerPlan;
import kr.inventory.domain.chat.service.context.ChatConversationContext;
import kr.inventory.global.llm.config.LlmProperties;
import kr.inventory.global.llm.dto.LlmExecutionOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatAnswerPlanningService {

    private static final int GEMINI_25_PRO_DEFAULT_THINKING_BUDGET = 2048;
    private static final int GEMINI_25_PRO_MIN_THINKING_BUDGET = 128;
    private static final int GEMINI_25_PRO_MAX_THINKING_BUDGET = 32768;
    private static final int GEMINI_25_FLASH_MAX_THINKING_BUDGET = 24576;

    private static final Set<String> REASONING_KEYWORDS = Set.of(
            "왜", "원인", "비교", "차이", "설계", "구조", "아키텍처", "트러블슈팅", "디버깅",
            "최적화", "전략", "가이드", "방법", "패턴", "리팩토링", "동시성", "품질",
            "분석", "정리", "고도화", "장단점", "문제", "해결"
    );

    private final LlmProperties llmProperties;

    public ChatAnswerPlan plan(ChatConversationContext context) {
        boolean complex = isComplexQuestion(context.currentUserMessage()) || context.followUpQuestion();
        boolean useReasoning = complex;

        if (useReasoning) {
            String reasoningModel = llmProperties.getReasoningModel();
            return new ChatAnswerPlan(
                    reasoningModel,
                    llmProperties.getReasoningTemperature(),
                    resolveThinkingBudget(reasoningModel, llmProperties.getReasoningThinkingBudget()),
                    llmProperties.getReasoningMaxOutputTokens(),
                    llmProperties.isReviewEnabled(),
                    complex ? 4 : 3,
                    context.followUpQuestion()
                            ? "Continue from the previous conversation context and thoroughly supplement missing comparison axes, rationale, and implementation steps."
                            : "Answer in depth, including not just the core conclusion but also rationale, design intent, trade-offs, and precautions."
            );
        }

        String flashModel = llmProperties.getFlashModel();
        return new ChatAnswerPlan(
                flashModel,
                llmProperties.getFlashTemperature(),
                resolveThinkingBudget(flashModel, llmProperties.getFlashThinkingBudget()),
                llmProperties.getFlashMaxOutputTokens(),
                complex && llmProperties.isReviewEnabled(),
                complex ? 3 : 2,
                context.followUpQuestion()
                        ? "Restore omitted subjects and comparison targets from the previous conversation and continue the answer quickly."
                        : "Answer quickly, but ensure core execution points are clear and no assumptions are missing."
        );
    }

    public LlmExecutionOptions toExecutionOptions(ChatAnswerPlan plan, boolean toolEnabled) {
        return new LlmExecutionOptions(
                plan.model(),
                plan.temperature(),
                resolveThinkingBudget(plan.model(), plan.thinkingBudget()),
                plan.maxOutputTokens(),
                toolEnabled
        );
    }

    public LlmExecutionOptions flashFallbackOptions(boolean toolEnabled) {
        String flashModel = llmProperties.getFlashModel();
        return new LlmExecutionOptions(
                flashModel,
                llmProperties.getFlashTemperature(),
                resolveThinkingBudget(flashModel, llmProperties.getFlashThinkingBudget()),
                llmProperties.getFlashMaxOutputTokens(),
                toolEnabled
        );
    }

    public boolean canFallbackToFlash(ChatAnswerPlan plan) {
        if (plan == null) {
            return false;
        }

        return !Objects.equals(plan.model(), llmProperties.getFlashModel())
                || positive(plan.thinkingBudget())
                || greaterThan(plan.maxOutputTokens(), llmProperties.getFlashMaxOutputTokens());
    }

    public LlmExecutionOptions reviewOptions() {
        String reviewModel = llmProperties.getReviewModel();
        return new LlmExecutionOptions(
                reviewModel,
                llmProperties.getReviewTemperature(),
                resolveThinkingBudget(reviewModel, 0),
                llmProperties.getReviewMaxOutputTokens(),
                false
        );
    }

    public int reviewMinAnswerLength() {
        return Math.max(0, llmProperties.getReviewMinAnswerLength());
    }

    private boolean isComplexQuestion(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }

        String normalized = message.trim();
        if (normalized.length() >= 100) {
            return true;
        }

        return REASONING_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private Integer resolveThinkingBudget(String model, Integer configuredBudget) {
        if (!StringUtils.hasText(model)) {
            return configuredBudget;
        }

        String normalizedModel = model.trim().toLowerCase();
        Integer budget = configuredBudget;

        if (normalizedModel.contains("gemini-2.5-pro")) {
            if (budget == null || budget == 0) {
                return GEMINI_25_PRO_DEFAULT_THINKING_BUDGET;
            }
            if (budget < 0) {
                return -1;
            }
            return clamp(budget, GEMINI_25_PRO_MIN_THINKING_BUDGET, GEMINI_25_PRO_MAX_THINKING_BUDGET);
        }

        if (normalizedModel.contains("gemini-2.5-flash")) {
            if (budget == null) {
                return 0;
            }
            if (budget < 0) {
                return -1;
            }
            return Math.min(budget, GEMINI_25_FLASH_MAX_THINKING_BUDGET);
        }

        return budget;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private boolean greaterThan(Integer left, Integer right) {
        if (left == null || right == null) {
            return false;
        }
        return left > right;
    }
}
