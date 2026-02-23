package kr.inventory.domain.catalog.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MenuCreateRequest(
        @NotBlank(message = "메뉴 이름은 필수입니다.")
        String name,

        @NotNull(message = "기본 가격은 필수입니다.")
        BigDecimal basePrice,

        JsonNode ingredientsJson
) {}
