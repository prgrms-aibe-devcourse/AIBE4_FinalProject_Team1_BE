package kr.inventory.ai.sales.tool.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TopMenuRankingToolResponse(
        String actionKey,
        String periodKey,
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        String rankBy,
        BigDecimal totalSalesAmount,
        int count,
        List<TopMenuRankingItemToolResponse> menus,
        List<SuggestedAction> suggestedFollowUps
) {
}
