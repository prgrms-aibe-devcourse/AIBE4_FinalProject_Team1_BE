package kr.inventory.domain.sales.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SalesOrderSearchRequest(
        OffsetDateTime from,
        OffsetDateTime to,
        SalesOrderStatus status,
        BigDecimal amountMin,
        BigDecimal amountMax
) {}
