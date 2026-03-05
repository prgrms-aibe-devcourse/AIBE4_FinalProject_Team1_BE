package kr.inventory.domain.stock.service.command;

import java.math.BigDecimal;
import java.util.Map;

public record StockDeductionRequest(
        Long storeId,
        Long salesOrderId,
        Map<Long, BigDecimal> usageMap
) {
    public static StockDeductionRequest of(Long storeId, Long salesOrderId, Map<Long, BigDecimal> usageMap) {
        return new StockDeductionRequest(storeId, salesOrderId, usageMap);
    }
}
