package kr.inventory.mcp.tool.stock;

import kr.inventory.mcp.application.stock.StockMcpQueryService;
import kr.inventory.mcp.context.ChatSessionContextProvider;
import kr.inventory.mcp.dto.stock.request.GetCurrentStockToolRequest;
import kr.inventory.mcp.dto.stock.response.GetCurrentStockToolResponse;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StockMcpTools {

    private final StockMcpQueryService stockMcpQueryService;
    private final ChatSessionContextProvider chatSessionContextProvider;

    /**
     * MCP Tool
     * 현재 재고 현황 조회
     */
    @McpTool(
            name = "get_current_stock_status",
            description = "현재 로그인한 사용자의 매장 재고 현황을 조회합니다. 필요하면 특정 원재료 이름으로 필터링할 수 있습니다."
    )
    public GetCurrentStockToolResponse getCurrentStockStatus(
            @McpToolParam(description = "재료 이름 검색어. 예: 우유", required = false)
            String keyword
    ) {
        Long userId = chatSessionContextProvider.getCurrentUserId();
        UUID storePublicId = chatSessionContextProvider.getCurrentStorePublicId();

        GetCurrentStockToolRequest request = new GetCurrentStockToolRequest(
                storePublicId,
                keyword
        );

        return stockMcpQueryService.getCurrentStockStatus(userId, request);
    }
}
