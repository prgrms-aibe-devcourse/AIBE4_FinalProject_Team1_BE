package kr.inventory.domain.analytics.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ReportSearchRequest(
        @NotNull(message = "시작일은 필수입니다")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @NotNull(message = "종료일은 필수입니다")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to
) {}
