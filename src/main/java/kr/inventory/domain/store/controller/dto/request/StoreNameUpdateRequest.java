package kr.inventory.domain.store.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreNameUpdateRequest(
    @NotBlank(message = "매장 이름은 필수입니다")
    @Size(max = 100, message = "매장 이름은 100자 이하여야 합니다")
    String name
) {
}
