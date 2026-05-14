package kr.inventory.domain.chat.service.context;

public record ChatAnswerPlan(
        String model,
        Double temperature,
        Integer thinkingBudget,
        Integer maxOutputTokens,
        boolean selfReviewEnabled,
        int minimumParagraphs,
        String directionGuide
) {
}
