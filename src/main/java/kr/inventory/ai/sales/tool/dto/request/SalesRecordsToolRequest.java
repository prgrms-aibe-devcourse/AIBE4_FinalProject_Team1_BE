package kr.inventory.ai.sales.tool.dto.request;

import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.sales.constant.SalesConstants;
import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.ai.sales.tool.support.SalesToolDateRangeResolver;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.domain.sales.service.command.SalesLedgerSortBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public record SalesRecordsToolRequest(
        DateRangePreset period,
        LocalDate fromDate,
        LocalDate toDate,
        String status,
        String type,
        String menuName,
        BigDecimal amountMin,
        BigDecimal amountMax,
        String tableCode,
        String sortBy,
        Integer page,
        Integer size
) {
    public SalesToolDateRange resolvedDateRange() {
        return SalesToolDateRangeResolver.resolve(period, fromDate, toDate, DateRangePreset.LAST_7_DAYS);
    }

    public int resolvedPageIndex() {
        if (page == null || page <= 0) {
            return SalesConstants.DEFAULT_PAGE - 1;
        }
        return page - 1;
    }

    public int resolvedSize() {
        if (size == null || size <= 0) {
            return SalesConstants.DEFAULT_SIZE;
        }
        return Math.min(size, SalesConstants.MAX_SIZE);
    }

    public String normalizedStatus() {
        return normalizeEnumString(status);
    }

    public String normalizedType() {
        return normalizeEnumString(type);
    }

    public String normalizedMenuName() {
        return normalizeText(menuName);
    }

    public String normalizedTableCode() {
        return normalizeText(tableCode);
    }

    public SalesOrderStatus resolvedStatus() {
        String normalizedStatus = normalizedStatus();
        if (normalizedStatus == null) {
            return null;
        }

        try {
            return SalesOrderStatus.valueOf(normalizedStatus);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public SalesOrderType resolvedType() {
        String normalizedType = normalizedType();
        if (normalizedType == null) {
            return null;
        }

        try {
            return SalesOrderType.valueOf(normalizedType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public BigDecimal resolvedAmountMin() {
        return amountMin;
    }

    public BigDecimal resolvedAmountMax() {
        return amountMax;
    }

    public SalesLedgerSortBy resolvedSortBy() {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            return SalesLedgerSortBy.ORDERED_AT_DESC;
        }

        String raw = sortBy.trim();
        String compact = raw.replace(" ", "").replace("-", "").toLowerCase(Locale.ROOT);

        return switch (compact) {
            case "최신순", "최근순", "주문최신순", "orderedatdesc" -> SalesLedgerSortBy.ORDERED_AT_DESC;
            case "오래된순", "과거순", "주문오래된순", "orderedatasc" -> SalesLedgerSortBy.ORDERED_AT_ASC;
            case "금액큰순", "큰금액순", "높은금액순", "총액큰순", "매출큰순", "가장비싼순", "totalamountdesc" ->
                    SalesLedgerSortBy.TOTAL_AMOUNT_DESC;
            case "금액작은순", "작은금액순", "낮은금액순", "총액작은순", "totalamountasc" ->
                    SalesLedgerSortBy.TOTAL_AMOUNT_ASC;
            default -> {
                String normalized = raw
                        .toUpperCase(Locale.ROOT)
                        .replace('-', '_')
                        .replace(' ', '_');
                try {
                    yield SalesLedgerSortBy.valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    yield SalesLedgerSortBy.ORDERED_AT_DESC;
                }
            }
        };
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEnumString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String trimmed = value.trim();
        String aliasKey = trimmed.replace(" ", "").replace("-", "").toLowerCase(Locale.ROOT);

        if (SalesConstants.STATUS_ALIASES.containsKey(aliasKey)) {
            return SalesConstants.STATUS_ALIASES.get(aliasKey);
        }
        if (SalesConstants.TYPE_ALIASES.containsKey(aliasKey)) {
            return SalesConstants.TYPE_ALIASES.get(aliasKey);
        }

        return trimmed
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
