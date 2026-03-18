package kr.inventory.ai.sales.tool.dto.response;

public record SuggestedAction(
        String actionKey,
        String label,
        String periodKey
) {
}
