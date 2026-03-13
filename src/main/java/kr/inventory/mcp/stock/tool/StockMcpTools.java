package kr.inventory.mcp.stock.tool;

import kr.inventory.domain.stock.controller.dto.response.LowStockIngredientResponse;
import kr.inventory.mcp.stock.service.StockMcpQueryService;
import kr.inventory.mcp.context.ChatSessionContextProvider;
import kr.inventory.mcp.stock.dto.request.GetCurrentStockToolRequest;
import kr.inventory.mcp.stock.dto.response.GetCurrentStockToolResponse;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
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

    @McpTool(
            name = "get_low_stock_ingredients",
            description = "현재 로그인한 사용자의 매장에서 재고가 임계치 미만은 재료 목록을 조회합니다."
    )
    public List<LowStockIngredientResponse> getLowStockIngredients() {
        Long userId = chatSessionContextProvider.getCurrentUserId();
        UUID storePublicId = chatSessionContextProvider.getCurrentStorePublicId();

        return stockMcpQueryService.getLowStockIngredients(userId, storePublicId);
    }
}
