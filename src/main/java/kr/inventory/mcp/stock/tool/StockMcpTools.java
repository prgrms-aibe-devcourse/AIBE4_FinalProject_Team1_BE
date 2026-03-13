package kr.inventory.mcp.stock.tool;

import kr.inventory.domain.stock.controller.dto.response.LowStockIngredientResponse;
import kr.inventory.mcp.context.ChatSessionContextProvider;
import kr.inventory.mcp.stock.service.StockMcpQueryService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StockMcpTools {

    private final StockMcpQueryService stockMcpQueryService;
    private final ChatSessionContextProvider chatSessionContextProvider;

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
