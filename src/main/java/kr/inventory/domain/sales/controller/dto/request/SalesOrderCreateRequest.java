package kr.inventory.domain.sales.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SalesOrderCreateRequest(
        @Valid
        @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다")
        List<SalesOrderItemRequest> items
) {}
