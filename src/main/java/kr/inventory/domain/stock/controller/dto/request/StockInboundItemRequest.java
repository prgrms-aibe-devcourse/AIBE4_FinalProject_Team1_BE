package kr.inventory.domain.stock.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockInboundItemRequest(
        @NotBlank
        @Size(max = 255)
        String rawProductName,

        @Positive
        BigDecimal quantity,

        @Positive
        BigDecimal unitCost,

        LocalDate expirationDate,

        @Size(max = 500)
        String specText
) {
}
