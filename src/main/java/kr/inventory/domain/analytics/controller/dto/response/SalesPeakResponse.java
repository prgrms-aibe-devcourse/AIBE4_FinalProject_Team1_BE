package kr.inventory.domain.analytics.controller.dto.response;

public record SalesPeakResponse(
        int dayOfWeek,    // 1(월) ~ 7(일)
        int hour,         // 0 ~ 23
        long orderCount
) {}
