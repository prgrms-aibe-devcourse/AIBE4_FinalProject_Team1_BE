package kr.inventory.ai.stock.tool;

import kr.inventory.ai.common.constant.ToolDescriptionConstants;
import kr.inventory.ai.common.dto.DateRange;
import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.common.resolver.DateRangeResolver;
import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.ai.stock.service.StockLogAiQueryService;
import kr.inventory.ai.stock.tool.dto.request.SearchStockLogsToolRequest;
import kr.inventory.ai.stock.tool.dto.response.SearchStockLogsToolResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockLogAiTools {

    private final StockLogAiQueryService stockLogAiQueryService;
    private final ChatToolContextProvider chatToolContextProvider;
    private final DateRangeResolver dateRangeResolver;

    @Tool(
            name = "search_stock_logs",
            description = """
                    재고 로그를 검색합니다.
                    이 툴은 원본 로그 조회 전용이며, 집계나 통계는 하지 않습니다.

                    조회할 수 있는 조건:
                    - keyword: 식재료명 또는 상품명 키워드
                    - transactionType: INBOUND, DEDUCTION, WASTE
                    - referenceType: SALE, WASTE, STOCK_TAKING, INBOUND 등
                    - period: 조회 기간 프리셋
                    - limit: 최대 조회 개수

                    반환 정보:
                    - 식재료명
                    - 상품명
                    - 거래 유형
                    - 참조 사유
                    - 변동 수량
                    - 변동 후 잔량
                    - 생성 시각

                    예:
                    - 오늘 발생한 로그들 보여줘
                    - 식빵 관련 재고 변동 내역 보여줘
                    - 판매로 차감된 로그만 보여줘
                    """
                    + ToolDescriptionConstants.DATE_RANGE_PRESET
    )
    public SearchStockLogsToolResponse searchStockLogs(
            @ToolParam(description = "Ingredient name or product name keyword")
            String keyword,
            @ToolParam(description = "Transaction type (INBOUND: 입고, DEDUCTION: 차감, WASTE: 폐기, ADJUST: 재고 실사(조정))")
            String transactionType,
            @ToolParam(description = "Reference type (SALE: 판매, WASTE: 폐기, STOCK_TAKING: 재고 실사(조정), INBOUND: 입고, OTHER: 그 외)")
            String referenceType,
            @ToolParam(description = "Date range preset")
            DateRangePreset period,
            @ToolParam(description = "Maximum number of logs to return")
            Integer limit
    ) {
        ChatToolContext context = chatToolContextProvider.getRequired();
        DateRange range = dateRangeResolver.resolve(period);

        SearchStockLogsToolRequest request = new SearchStockLogsToolRequest(
                keyword,
                transactionType,
                referenceType,
                range.from(),
                range.to(),
                limit
        );

        return stockLogAiQueryService.searchStockLogs(
                context.userId(),
                context.storePublicId(),
                request
        );
    }
}
