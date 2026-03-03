package kr.inventory.domain.sales.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SalesOrderItemRequest(
        @NotNull(message = "메뉴 ID는 필수입니다")
        UUID menuPublicId,

        @NotNull(message = "수량은 필수입니다")
        @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다")
        Integer quantity
) {}
