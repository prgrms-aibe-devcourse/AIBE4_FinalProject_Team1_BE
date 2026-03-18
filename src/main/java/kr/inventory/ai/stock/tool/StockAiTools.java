package kr.inventory.ai.stock.tool;

import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.ai.stock.service.CurrentStockAiQueryService;
import kr.inventory.ai.stock.service.StockAiQueryService;
import kr.inventory.ai.stock.service.StockBatchAiQueryService;
import kr.inventory.ai.stock.tool.dto.request.CurrentStockOverviewToolRequest;
import kr.inventory.ai.stock.tool.dto.request.GetStockBatchesByIngredientToolRequest;
import kr.inventory.ai.stock.tool.dto.response.CurrentStockOverviewToolResponse;
import kr.inventory.ai.stock.tool.dto.response.GetStockBatchesByIngredientToolResponse;
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
    private final StockBatchAiQueryService stockBatchAiQueryService;
    private final ChatToolContextProvider chatToolContextProvider;

    @Tool(
            name = "get_low_stock_ingredients",
            description = """
                    현재 로그인한 사용자의 매장에서 재고가 임계치 미만인 재료 목록을 조회합니다.
                    """
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
                - keyword: 재료명 검색 (optional)
                - status: 재고 상태 필터 (OUT_OF_STOCK, LOW_STOCK, NORMAL) (optional)
                - sortBy: 정렬 기준 (STOCK_ASC, STOCK_DESC, NAME_ASC, NAME_DESC) (optional, 기본값: NAME_ASC)
                - limit: 최대 조회 개수 (optional, 기본값: null, 최대: 100)

                중요:
                - 사용자가 개수를 명시하지 않으면 limit 없이 호출하세요.
                - limit가 없으면 기본값 null을 반환합니다.
                - 사용자가 "전체", "전부", "다 보여줘"라고 하면 limit는 100으로 해석하세요.

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

    @Tool(
            name = "get_stock_batches_by_ingredient",
            description = """
                    특정 재료의 배치별 재고를 조회합니다.

                    사용 목적:
                    - 어떤 배치가 먼저 소진되어야 하는지 확인
                    - 유통기한이 빠른 배치 확인
                    - 배치별 남은 수량 확인

                    입력:
                    - keyword: 재료명 키워드

                    반환:
                    - 재료 정보
                    - 배치 ID
                    - 남은 수량
                    - 입고일
                    - 유통기한
                    - 유통기한까지 남은 날짜
                    - 배치 상태

                    유통기한이 빠른 순으로 정렬해서 반환합니다.
                    """
    )
    public GetStockBatchesByIngredientToolResponse getStockBatchesByIngredient(
            @ToolParam(description = "Ingredient name keyword") String keyword
    ) {
        ChatToolContext context = chatToolContextProvider.getRequired();

        GetStockBatchesByIngredientToolRequest request =
                new GetStockBatchesByIngredientToolRequest(keyword);

        return stockBatchAiQueryService.getStockBatchesByIngredient(
                context.userId(),
                context.storePublicId(),
                request
        );
    }
}
