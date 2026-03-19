package kr.inventory.ai.sales.tool.support;

import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.sales.constant.SalesConstants;
import kr.inventory.ai.sales.exception.SalesErrorCode;
import kr.inventory.ai.sales.exception.SalesException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class SalesToolDateRangeResolver {

    private SalesToolDateRangeResolver() {
    }

    public static SalesToolDateRange resolve(String preset, String fromDate, String toDate, String defaultPreset) {
        boolean hasFrom = hasText(fromDate);
        boolean hasTo = hasText(toDate);

        if (hasFrom || hasTo) {
            if (!hasFrom || !hasTo) {
                throw new SalesException(SalesErrorCode.BOTH_DATES_REQUIRED);
            }

            LocalDate resolvedFrom = parseDate(fromDate, "fromDate");
            LocalDate resolvedTo = parseDate(toDate, "toDate");

            if (resolvedFrom.isAfter(resolvedTo)) {
                throw new SalesException(SalesErrorCode.INVALID_DATE_RANGE);
            }

            return toRange("custom", resolvedFrom, resolvedTo);
        }

        String normalizedPreset = normalizePreset(hasText(preset) ? preset : defaultPreset);
        LocalDate today = LocalDate.now(SalesConstants.KST);

        return switch (normalizedPreset) {
            case "today" -> toRange("today", today, today);
            case "yesterday" -> {
                LocalDate yesterday = today.minusDays(1);
                yield toRange("yesterday", yesterday, yesterday);
            }
            case "this_week" -> toRange("this_week", today.minusDays(today.getDayOfWeek().getValue() - 1L), today);
            case "this_month" -> toRange("this_month", today.withDayOfMonth(1), today);
            case "last_7_days" -> toRange("last_7_days", today.minusDays(6), today);
            case "last_30_days" -> toRange("last_30_days", today.minusDays(29), today);
            case "last_month" -> {
                LocalDate firstDayOfThisMonth = today.withDayOfMonth(1);
                LocalDate lastMonthDate = firstDayOfThisMonth.minusDays(1);
                yield toRange("last_month", lastMonthDate.withDayOfMonth(1), lastMonthDate);
            }
            default -> throw new SalesException(SalesErrorCode.UNSUPPORTED_PRESET);
        };
    }

    public static SalesToolDateRange resolve(
            DateRangePreset preset,
            LocalDate fromDate,
            LocalDate toDate,
            DateRangePreset defaultPreset
    ) {
        boolean hasFrom = fromDate != null;
        boolean hasTo = toDate != null;

        if (hasFrom || hasTo) {
            if (!hasFrom || !hasTo) {
                throw new SalesException(SalesErrorCode.BOTH_DATES_REQUIRED);
            }
            if (fromDate.isAfter(toDate)) {
                throw new SalesException(SalesErrorCode.INVALID_DATE_RANGE);
            }
            return toRange("custom", fromDate, toDate);
        }

        String resolvedPreset = preset != null
                ? preset.getValue()
                : defaultPreset != null ? defaultPreset.getValue() : DateRangePreset.LAST_7_DAYS.getValue();

        return resolve(resolvedPreset, null, null, DateRangePreset.LAST_7_DAYS.getValue());
    }

    public static LocalDate parseLocalDate(String value, String fieldName) {
        return parseDate(value, fieldName);
    }

    private static SalesToolDateRange toRange(String preset, LocalDate fromDate, LocalDate toDate) {
        OffsetDateTime fromDateTime = fromDate.atStartOfDay(SalesConstants.KST).toOffsetDateTime();
        OffsetDateTime toDateTime = toDate.atTime(23, 59, 59).atZone(SalesConstants.KST).toOffsetDateTime();

        return new SalesToolDateRange(preset, fromDate, toDate, fromDateTime, toDateTime);
    }

    private static LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim(), SalesConstants.ISO_DATE);
        } catch (DateTimeParseException exception) {
            throw new SalesException(SalesErrorCode.INVALID_DATE_FORMAT);
        }
    }

    private static String normalizePreset(String preset) {
        return preset.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
