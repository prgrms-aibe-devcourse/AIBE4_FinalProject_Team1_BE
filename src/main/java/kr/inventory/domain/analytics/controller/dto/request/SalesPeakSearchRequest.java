package kr.inventory.domain.analytics.controller.dto.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

public record SalesPeakSearchRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
) {}
