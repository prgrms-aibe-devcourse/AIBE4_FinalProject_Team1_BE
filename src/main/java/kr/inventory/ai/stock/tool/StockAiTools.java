package kr.inventory.ai.stock.tool;

import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.ai.stock.service.StockAiQueryService;
import kr.inventory.ai.stock.service.StockInboundAiQueryService;
import kr.inventory.ai.stock.tool.dto.request.InboundListToolRequest;
import kr.inventory.ai.stock.tool.dto.response.InboundListToolResponse;
import kr.inventory.ai.stock.tool.dto.response.LowStockIngredientResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StockAiTools {

    private final StockAiQueryService stockAiQueryService;
    private final StockInboundAiQueryService stockInboundAiQueryService;
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
            name = "get_inbound_list",
            description = """
                    현재 로그인한 사용자의 매장에서 최근 입고 목록을 조회합니다.
                    status로 입고 상태를 필터링할 수 있고, keyword로 거래처명 또는 품목명을 검색할 수 있습니다.
                    결과에는 입고 ID, 입고일, 상태, 거래처명, 품목 수, 확정 여부, 확정자, 확정 시각, 정규화 필요 여부가 포함됩니다.
                    최근 입고 내역 확인, 정규화 필요 입고 탐색, 확정 여부 확인이 필요할 때 사용합니다.
                    """
    )
    public InboundListToolResponse getInboundList(InboundListToolRequest request) {
        ChatToolContext context = chatToolContextProvider.getRequired();

        return stockInboundAiQueryService.getInboundList(
                context.userId(),
                context.storePublicId(),
                request
        );
    }
}
