package kr.dontworry.domain.category.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotNull(message = "ledger ID는 필수입니다.")
        Long ledgerId,

        @NotBlank(message = "카테고리 이름은 필수입니다.")
        @Size(max = 30, message = "카테고리 이름은 30자 이하여야 합니다.")
        String name,

        @Size(max = 100, message = "아이콘은 100자 이하여야 합니다.")
        String icon,

        @Size(max = 30, message = "색상은 30자 이하여야 합니다.")
        String color,

        @NotNull(message = "정렬 순서는 필수입니다.")
        Integer sortOrder
) {
}