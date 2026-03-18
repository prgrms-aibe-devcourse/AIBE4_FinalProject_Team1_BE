package kr.inventory.ai.sales.tool.support;

import kr.inventory.ai.sales.constant.SalesConstants;
import kr.inventory.ai.sales.exception.SalesErrorCode;
import kr.inventory.ai.sales.exception.SalesException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class SalesToolDateRangeResolver {

    private static final ZoneId KST = SalesConstants.KST;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

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
        LocalDate today = LocalDate.now(KST);

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

    public static LocalDate parseLocalDate(String value, String fieldName) {
        return parseDate(value, fieldName);
    }

    private static SalesToolDateRange toRange(String preset, LocalDate fromDate, LocalDate toDate) {
        OffsetDateTime fromDateTime = fromDate.atStartOfDay(KST).toOffsetDateTime();
        OffsetDateTime toDateTime = toDate.atTime(23, 59, 59).atZone(KST).toOffsetDateTime();

        return new SalesToolDateRange(preset, fromDate, toDate, fromDateTime, toDateTime);
    }

    private static LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim(), ISO_DATE);
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
