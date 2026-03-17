package kr.inventory.ai.stock.tool;

import kr.inventory.ai.common.constant.ToolDescriptionConstants;
import kr.inventory.ai.common.dto.DateRange;
import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.common.resolver.DateRangeResolver;
import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.ai.stock.service.StockShortageAiQueryService;
import kr.inventory.ai.stock.tool.dto.response.StockShortageSummaryToolResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class StockShortageAiTools {
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Seoul");

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
        DateRange range = dateRangeResolver.resolve(period, DEFAULT_ZONE_ID);

        return stockShortageAiQueryService.getStockShortageSummary(
                context.userId(),
                context.storePublicId(),
                keyword,
                range.from(),
                range.to()
        );
    }
}