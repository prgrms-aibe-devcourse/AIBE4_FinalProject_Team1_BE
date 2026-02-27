package kr.inventory.domain.dining.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TableSessionEnterRequest(
        @NotNull UUID storePublicId,
        @NotNull UUID tablePublicId
) {
}
