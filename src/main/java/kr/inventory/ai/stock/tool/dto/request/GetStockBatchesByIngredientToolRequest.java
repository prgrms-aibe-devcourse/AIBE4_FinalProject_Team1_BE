package kr.inventory.ai.stock.tool.dto.request;

public record GetStockBatchesByIngredientToolRequest(
        String keyword
) {
    public String normalizedKeyword() {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
