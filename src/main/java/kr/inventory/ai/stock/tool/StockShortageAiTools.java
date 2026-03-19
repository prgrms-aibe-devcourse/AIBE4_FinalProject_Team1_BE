package kr.inventory.ai.stock.tool;

import kr.inventory.ai.common.constant.ToolDescriptionConstants;
import kr.inventory.ai.common.dto.DateRange;
import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.common.resolver.DateRangeResolver;
import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.ai.stock.service.StockShortageAiQueryService;
import kr.inventory.ai.stock.tool.dto.response.ShortageRelatedOrderToolResponse;
import kr.inventory.ai.stock.tool.dto.response.StockShortageSummaryToolResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StockShortageAiTools {;
    private final StockShortageAiQueryService stockShortageAiQueryService;
    private final ChatToolContextProvider chatToolContextProvider;
    private final DateRangeResolver dateRangeResolver;

	@Tool(
		name = "get_stock_shortage_summary",
		description = """
			재고 부족 이력을 조회합니다.
			
            사용 방법:
            - 특정 원재료명을 keyword로 검색할 수 있습니다.
            - status로 PENDING 또는 CLOSED 상태를 필터링할 수 있습니다.
			- 기간은 반드시 아래 period 프리셋 값만 사용합니다.
			""" + ToolDescriptionConstants.DATE_RANGE_PRESET + """
    
            응답 작성 규칙:
            - 먼저 어떤 재료(ingredientName)에 대한 부족 건수인지 표시합니다.
            - 그 후 재고 부족량(totalShortageAmount)과 해당되는 주문의 수(affectedOrderCount)를 보여줍니다.
            - 그 다음 상태를 보여주는데, PENDING이라면 '부족', CLOSED라면 '해결'이라고 보여줍니다.
            - shortages가 비어 있으면 "조회된 재고 부족 이력이 없습니다."처럼 명확하게 안내합니다.
            - stockShortagePublicId는 내부 식별자이므로 특별히 필요하지 않으면 노출하지 않습니다.
            """
	)
	public StockShortageSummaryToolResponse getStockShortageSummary(
		@ToolParam(description = "Ingredient name keyword")
		String keyword,
		@ToolParam(description = "Date range preset")
        String period,
		@ToolParam(description = "StockShortage status (ex: PENDING, CLOSED)")
		String status
	) {
		ChatToolContext context = chatToolContextProvider.getRequired();

        DateRangePreset preset = DateRangePreset.from(period);
        DateRange range = dateRangeResolver.resolve(preset);

		return stockShortageAiQueryService.getStockShortageSummary(
			context.userId(),
			context.storePublicId(),
			keyword,
			status,
			range.from(),
			range.to()
		);
	}

    // 해당 tool의 사용 유무 고민
    @Tool(
            name = "get_shortage_related_order",
            description = """
                    특정 재고 부족 건과 연결된 주문 요약 정보를 조회합니다.
                    shortage public id에 대한 부족 정보와 관련 주문의 핵심 정보를 반환합니다.
                    """
    )
    public ShortageRelatedOrderToolResponse getShortageRelatedOrder(
            @ToolParam(description = "Stock shortage public id")
            UUID shortagePublicId
    ) {
        ChatToolContext context = chatToolContextProvider.getRequired();

        return stockShortageAiQueryService.getShortageRelatedOrder(
                context.userId(),
                context.storePublicId(),
                shortagePublicId
        );
    }
}