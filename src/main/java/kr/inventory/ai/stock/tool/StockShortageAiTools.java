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
                    """ + ToolDescriptionConstants.DATE_RANGE_PRESET
    )
    public StockShortageSummaryToolResponse getStockShortageSummary(
            @ToolParam(description = "Ingredient name keyword")
            String keyword,
            @ToolParam(description = "Date range preset")
            DateRangePreset period
    ) {
        ChatToolContext context = chatToolContextProvider.getRequired();
        DateRange range = dateRangeResolver.resolve(period);

        return stockShortageAiQueryService.getStockShortageSummary(
                context.userId(),
                context.storePublicId(),
                keyword,
                range.from(),
                range.to()
        );
    }

    @Tool(
            name = "get_shortage_related_order",
            description = """
                    특정 재고 부족 건과 연결된 주문 요약 정보를 조회합니다.
                    shortage public id를 입력하면 부족 정보와 관련 주문의 핵심 정보를 반환합니다.
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