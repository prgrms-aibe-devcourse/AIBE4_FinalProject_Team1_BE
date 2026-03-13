package kr.inventory.mcp.stock.dto.request;

import java.util.UUID;

public record GetCurrentStockToolRequest(
        UUID storePublicId,
        String keyword
) {
}
