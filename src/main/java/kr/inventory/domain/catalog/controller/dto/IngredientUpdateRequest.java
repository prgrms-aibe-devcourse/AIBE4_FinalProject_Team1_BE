package kr.inventory.domain.catalog.controller.dto;

import kr.inventory.domain.catalog.entity.enums.IngredientStatus;
import kr.inventory.domain.catalog.entity.enums.IngredientUnit;

import java.math.BigDecimal;

public record IngredientUpdateRequest(
        String name,
        IngredientUnit unit,
        BigDecimal lowStockThreshold,
        IngredientStatus status
) {}
