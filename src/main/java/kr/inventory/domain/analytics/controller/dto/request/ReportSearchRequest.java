package kr.inventory.domain.analytics.controller.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReportSearchRequest(
        @NotNull(message = "시작일은 필수입니다")
        LocalDate from,

        @NotNull(message = "종료일은 필수입니다")
        LocalDate to
) {}
