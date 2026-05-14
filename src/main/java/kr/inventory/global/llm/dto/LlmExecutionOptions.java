package kr.inventory.global.llm.dto;

public record LlmExecutionOptions(
        String model,
        Double temperature,
        Integer thinkingBudget,
        Integer maxOutputTokens,
        boolean toolEnabled
) {
}
