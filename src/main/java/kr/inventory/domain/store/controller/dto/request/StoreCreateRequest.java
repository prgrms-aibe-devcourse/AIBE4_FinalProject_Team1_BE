package kr.inventory.domain.store.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StoreCreateRequest(
    @NotBlank(message = "매장 이름은 필수입니다")
    @Size(max = 100, message = "매장 이름은 100자 이하여야 합니다")
    String name,

    @NotBlank(message = "사업자등록번호는 필수입니다")
    @Pattern(
        regexp = "^[0-9-]{10,12}$",
        message = "사업자등록번호는 10자리 숫자 또는 하이픈 포함 형식이어야 합니다"
    )
    String businessRegistrationNumber
) {
}
