package kr.inventory.domain.reference.controller.dto;

import kr.inventory.domain.reference.entity.enums.IngredientStatus;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;

import java.math.BigDecimal;

public record IngredientUpdateRequest(
        String name,
        IngredientUnit unit,
        BigDecimal lowStockThreshold,
        IngredientStatus status
) {}
