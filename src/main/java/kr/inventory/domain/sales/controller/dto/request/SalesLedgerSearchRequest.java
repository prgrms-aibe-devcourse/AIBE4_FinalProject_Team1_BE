package kr.inventory.domain.sales.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;

import java.time.OffsetDateTime;

public record SalesLedgerSearchRequest(
        @NotNull(message = "조회 시작일시는 필수입니다")
        OffsetDateTime from,

        @NotNull(message = "조회 종료일시는 필수입니다")
        OffsetDateTime to,

        SalesOrderStatus status,
        SalesOrderType type
) {
}
