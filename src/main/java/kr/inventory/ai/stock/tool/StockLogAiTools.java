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
                    재고 변동 원본 로그를 조건에 맞게 검색합니다.
                    이 툴은 개별 로그 조회 전용이며, 집계나 통계는 하지 않습니다.
                    
                    주로 다음과 같은 질문에 사용합니다:
                    - 특정 식재료/상품의 최근 재고 변동 내역을 보고 싶을 때
                    - 입고, 차감, 폐기, 조정 중 특정 유형의 로그만 보고 싶을 때
                    - 판매, 폐기, 입고, 재고 실사 등 특정 사유의 로그만 보고 싶을 때
                    - 특정 기간 동안 발생한 로그를 최근순으로 일부 확인하고 싶을 때
                    
                    이 툴은 전체 로그를 모두 보여주기 위한 용도가 아닙니다.
                    항상 조건 기반으로 최근 로그를 조회하며, 결과는 limit 만큼만 반환합니다.
                    사용자가 조건을 주지 않으면 전체 기간의 모든 로그를 조회하려 하지 말고,
                    최근 로그를 소량만 조회하거나, 더 적합한 요약/목록 툴이 있으면 그 툴을 우선 사용하세요.

                    조회할 수 있는 조건:
                    - keyword: 식재료명 또는 상품명 키워드
                    - transactionType: INBOUND, DEDUCTION, WASTE, ADJUST
                    - referenceType: SALE, WASTE, STOCK_TAKING, INBOUND, OTHER
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
