package kr.inventory.ai.sales.tool.support;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SalesToolDateRange(
        String preset,
        LocalDate fromDate,
        LocalDate toDate,
        OffsetDateTime fromDateTime,
        OffsetDateTime toDateTime
) {
}
