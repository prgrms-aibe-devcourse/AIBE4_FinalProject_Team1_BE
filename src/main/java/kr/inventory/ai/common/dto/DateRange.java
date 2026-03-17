package kr.inventory.ai.common.dto;

import java.time.OffsetDateTime;

public record DateRange(
        OffsetDateTime from,
        OffsetDateTime to
) {
    public static DateRange empty() {
        return new DateRange(null, null);
    }
}
