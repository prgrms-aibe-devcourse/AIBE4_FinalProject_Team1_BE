package kr.inventory.mcp.dto.stock.request;

import java.util.UUID;

public record GetCurrentStockToolRequest(
        UUID storePublicId,
        String keyword
) {
}
