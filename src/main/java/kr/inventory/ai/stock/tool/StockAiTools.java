package kr.inventory.ai.stock.tool;

import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.ai.stock.service.CurrentStockAiQueryService;
import kr.inventory.ai.stock.service.StockAiQueryService;
import kr.inventory.ai.stock.tool.dto.request.CurrentStockOverviewToolRequest;
import kr.inventory.ai.stock.tool.dto.response.CurrentStockOverviewToolResponse;
import kr.inventory.ai.stock.tool.dto.response.LowStockIngredientResponse;
import kr.inventory.ai.stock.tool.enums.StockOverviewSortBy;
import kr.inventory.ai.stock.tool.enums.StockOverviewStatusFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockAiTools {

    private final StockAiQueryService stockAiQueryService;
    private final CurrentStockAiQueryService currentStockAiQueryService;
    private final ChatToolContextProvider chatToolContextProvider;

    @Tool(
            name = "get_low_stock_ingredients",
            description = "현재 로그인한 사용자의 매장에서 재고가 임계치 미만인 재료 목록을 조회합니다."
    )
    public List<LowStockIngredientResponse> getLowStockIngredients() {
        ChatToolContext context = chatToolContextProvider.getRequired();

        return stockAiQueryService.getLowStockIngredients(
                context.userId(),
                context.storePublicId()
        );
    }

    @Tool(
            name = "get_current_stock_overview",
            description = """
                    현재 재고 현황을 조회합니다.
                    재료별 현재 재고 수량, 단위, 임계치, 재고 상태를 목록으로 반환합니다.

                    사용할 수 있는 조건:
                    - keyword: 재료명 검색
                    - status: 재고 상태 필터 (OUT_OF_STOCK, LOW_STOCK, NORMAL)
                    - sortBy: 정렬 기준 (STOCK_ASC, STOCK_DESC, NAME_ASC, NAME_DESC)
                    - limit: 최대 조회 개수

                    이 툴은 현재 재고의 목록/개요 조회용입니다.
                    배치별 상세 정보나 재고 이력은 포함하지 않습니다.
                    """
    )
    public CurrentStockOverviewToolResponse getCurrentStockOverview(
            @ToolParam(description = "Ingredient name keyword")
            String keyword,
            @ToolParam(description = "Stock status filter")
            StockOverviewStatusFilter status,
            @ToolParam(description = "Sort option")
            StockOverviewSortBy sortBy,
            @ToolParam(description = "Maximum number of items to return")
            Integer limit
    ) {
        ChatToolContext context = chatToolContextProvider.getRequired();

        CurrentStockOverviewToolRequest request = new CurrentStockOverviewToolRequest(
                keyword,
                status,
                sortBy,
                limit
        );

        return currentStockAiQueryService.getCurrentStockOverview(
                context.userId(),
                context.storePublicId(),
                request
        );
    }
}
