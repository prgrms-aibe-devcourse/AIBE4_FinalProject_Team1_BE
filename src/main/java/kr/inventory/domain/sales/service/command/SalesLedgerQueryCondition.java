package kr.inventory.domain.sales.service.command;

import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SalesLedgerQueryCondition(
        OffsetDateTime from,
        OffsetDateTime to,
        SalesOrderStatus status,
        SalesOrderType type,
        String menuName,
        BigDecimal amountMin,
        BigDecimal amountMax,
        String tableCode,
        SalesLedgerSortBy sortBy
) {
    public SalesLedgerQueryCondition {
        sortBy = sortBy != null ? sortBy : SalesLedgerSortBy.ORDERED_AT_DESC;
    }
}
