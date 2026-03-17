package kr.inventory.ai.stock.tool;

import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.ai.stock.service.StockShortageAiQueryService;
import kr.inventory.ai.stock.tool.dto.response.StockShortageSummaryToolResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class StockShortageAiTools {

    private final StockShortageAiQueryService stockShortageAiQueryService;
    private final ChatToolContextProvider chatToolContextProvider;

    @Tool(
            name = "get_stock_shortage_summary",
            description = """
            재고 부족 이력을 조회합니다.
            - If the user mentions relative dates like 'today', convert them to ISO-8601 strings based on the system time.
            - format example: 2026-03-17T00:00:00+09:00
            - If no specific date is mentioned, leave 'from' and 'to' as null.
            """
    )
    public StockShortageSummaryToolResponse getStockShortageSummary(
            @ToolParam(description = "Ingredient name keyword") String keyword,
            @ToolParam(description = "Start time (ISO-8601 format)") OffsetDateTime from,
            @ToolParam(description = "End time (ISO-8601 format)") OffsetDateTime to
    ) {
        ChatToolContext context = chatToolContextProvider.getRequired();

        return stockShortageAiQueryService.getStockShortageSummary(
                context.userId(),
                context.storePublicId(),
                keyword,
                from,
                to
        );
    }
}