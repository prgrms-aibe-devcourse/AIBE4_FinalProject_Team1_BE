package kr.inventory.domain.purchase.controller.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PurchaseOrderItemRequest(
        @NotBlank(message = "itemName은 필수입니다.")
        String itemName,
        @NotNull(message = "quantity는 필수입니다.")
        @Positive(message = "quantity는 1 이상이어야 합니다.")
        Integer quantity,
        @NotNull(message = "unitPrice는 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = false, message = "unitPrice는 0보다 커야 합니다.")
        BigDecimal unitPrice
) {
}
