package kr.inventory.mcp.stock.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import kr.inventory.mcp.stock.dto.response.GetCurrentStockToolResponse;
import kr.inventory.mcp.stock.tool.StockMcpTools;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mcp-test/stock")
@RequiredArgsConstructor
public class StockMcpTestController {

    private final StockMcpTools stockMcpTools;

    @Operation(
            summary = "현재 재고 현황 MCP Tool 테스트",
            description = "로그인 정보와 X-Store-Public-Id 헤더를 이용해 현재 재고 현황 MCP Tool을 테스트합니다."
    )
    @GetMapping("/current")
    public GetCurrentStockToolResponse getCurrentStockStatus(
            @Parameter(description = "현재 매장 Public ID", required = true)
            @RequestHeader("X-Store-Public-Id")
            String storePublicId,
            @RequestParam(required = false) String keyword
    ) {
        return stockMcpTools.getCurrentStockStatus(keyword);
    }
}