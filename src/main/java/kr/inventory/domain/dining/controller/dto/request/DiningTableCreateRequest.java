package kr.inventory.domain.dining.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DiningTableCreateRequest(
        @NotBlank(message = "테이블 코드는 필수입니다.")
        String tableCode
) {}