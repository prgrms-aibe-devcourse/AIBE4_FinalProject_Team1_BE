package kr.inventory.domain.catalog.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.inventory.domain.catalog.entity.enums.IngredientUnit;

import java.math.BigDecimal;

public record IngredientCreateRequest(
        @NotBlank(message = "식재료 이름은 필수입니다.")
        String name,

        @NotNull(message = "단위는 필수입니다.")
        IngredientUnit unit,

        BigDecimal lowStockThreshold
) {}
