package kr.inventory.domain.catalog.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import kr.inventory.domain.catalog.entity.Menu;
import kr.inventory.domain.catalog.entity.enums.MenuStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuResponse(
        UUID menuPublicId,
        String name,
        BigDecimal basePrice,
        MenuStatus status,
        JsonNode ingredientsJson
) {
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getMenuPublicId(),
                menu.getName(),
                menu.getBasePrice(),
                menu.getStatus(),
                menu.getIngredientsJson()
        );
    }
}
