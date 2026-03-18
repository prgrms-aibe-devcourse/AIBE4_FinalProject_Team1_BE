package kr.inventory.ai.common.resolver;

import kr.inventory.ai.common.dto.DateRange;
import kr.inventory.ai.common.enums.DateRangePreset;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class DateRangeResolver {
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Seoul");

    public DateRange resolve(DateRangePreset preset) {
        return resolve(preset, DEFAULT_ZONE_ID);
    }

    public DateRange resolve(DateRangePreset preset, ZoneId zoneId) {
        if (preset == null) {
            return DateRange.empty();
        }

        LocalDate today = LocalDate.now(zoneId);

        return switch (preset) {
            case TODAY -> rangeOfDay(today, zoneId);
            case YESTERDAY -> rangeOfDay(today.minusDays(1), zoneId);
            case THIS_WEEK -> rangeOfThisWeek(today, zoneId);
            case THIS_MONTH -> rangeOfThisMonth(today, zoneId);
            case LAST_7_DAYS -> rangeFromDaysAgo(today, 6, zoneId);
            case LAST_30_DAYS -> rangeFromDaysAgo(today, 29, zoneId);
            case LAST_MONTH -> rangeOfLastMonth(today, zoneId);
        };
    }

    private DateRange rangeOfDay(LocalDate date, ZoneId zoneId) {
        OffsetDateTime from = date.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime to = date.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();
        return new DateRange(from, to);
    }

    private DateRange rangeOfThisWeek(LocalDate today, ZoneId zoneId) {
        LocalDate start = today.with(DayOfWeek.MONDAY);
        LocalDate endExclusive = today.plusDays(1);

        return new DateRange(
                start.atStartOfDay(zoneId).toOffsetDateTime(),
                endExclusive.atStartOfDay(zoneId).toOffsetDateTime()
        );
    }

    private DateRange rangeOfThisMonth(LocalDate today, ZoneId zoneId) {
        LocalDate start = today.withDayOfMonth(1);
        LocalDate endExclusive = today.plusDays(1);

        return new DateRange(
                start.atStartOfDay(zoneId).toOffsetDateTime(),
                endExclusive.atStartOfDay(zoneId).toOffsetDateTime()
        );
    }

    private DateRange rangeFromDaysAgo(LocalDate today, int daysAgo, ZoneId zoneId) {
        LocalDate start = today.minusDays(daysAgo);
        LocalDate endExclusive = today.plusDays(1);

        return new DateRange(
                start.atStartOfDay(zoneId).toOffsetDateTime(),
                endExclusive.atStartOfDay(zoneId).toOffsetDateTime()
        );
    }

    private DateRange rangeOfLastMonth(LocalDate today, ZoneId zoneId) {
        LocalDate firstDayThisMonth = today.withDayOfMonth(1);
        LocalDate firstDayLastMonth = firstDayThisMonth.minusMonths(1);

        return new DateRange(
                firstDayLastMonth.atStartOfDay(zoneId).toOffsetDateTime(),
                firstDayThisMonth.atStartOfDay(zoneId).toOffsetDateTime()
        );
    }
}