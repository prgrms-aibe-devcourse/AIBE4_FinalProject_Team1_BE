package kr.inventory.domain.stock.controller.dto.request;

import java.math.BigDecimal;

public record UpdateNormalizationRequest(
    String normalizedUnit,

    BigDecimal normalizedUnitSize,

    String specText
) {
}
