package kr.inventory.domain.vendor.controller.dto;

import jakarta.validation.constraints.*;

public record VendorCreateRequest(
        @NotBlank(message = "거래처명은 필수입니다")
        @Size(max = 120, message = "거래처명은 120자 이하여야 합니다")
        String name,

        @Size(max = 100, message = "담당자명은 100자 이하여야 합니다")
        String contactPerson,

        @Size(max = 20, message = "연락처는 20자 이하여야 합니다")
        String phone,

        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다")
        String email,

        @Min(value = 1, message = "리드타임은 최소 1일입니다")
        @Max(value = 365, message = "리드타임은 최대 365일입니다")
        Integer leadTimeDays
) {
}
