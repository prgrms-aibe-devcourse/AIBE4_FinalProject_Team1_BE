package kr.inventory.domain.reference.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import kr.inventory.domain.reference.entity.enums.MenuStatus;

import java.math.BigDecimal;

public record MenuUpdateRequest(
        String name,
        BigDecimal basePrice,
        MenuStatus status,
        JsonNode ingredientsJson
) {}
