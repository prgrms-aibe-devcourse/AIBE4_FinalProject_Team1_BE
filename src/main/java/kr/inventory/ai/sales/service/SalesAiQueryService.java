package kr.inventory.ai.sales.service;

import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.sales.constant.SalesConstants;
import kr.inventory.ai.sales.exception.SalesErrorCode;
import kr.inventory.ai.sales.exception.SalesException;
import kr.inventory.ai.sales.support.SalesSuggestedActionProvider;
import kr.inventory.ai.sales.tool.dto.request.MenuSalesDetailToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesOrderDetailToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesPeakToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesRecordsToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesRefundSummaryToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesSummaryToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesTrendToolRequest;
import kr.inventory.ai.sales.tool.dto.request.TopMenuRankingToolRequest;
import kr.inventory.ai.sales.tool.dto.response.MenuSalesDetailToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesOrderDetailToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesPeakDayToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesPeakHourToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesPeakItemToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesPeakToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesRecordItemToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesRecordsPageInfoToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesRecordsSummaryToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesRecordsToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesRefundSummaryToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesSummaryToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesTrendPointToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesTrendToolResponse;
import kr.inventory.ai.sales.tool.dto.response.TopMenuRankingItemToolResponse;
import kr.inventory.ai.sales.tool.dto.response.TopMenuRankingToolResponse;
import kr.inventory.ai.sales.tool.support.SalesToolDateRange;
import kr.inventory.ai.sales.tool.support.SalesToolDateRangeResolver;
import kr.inventory.domain.analytics.controller.dto.response.MenuRankingResponse;
import kr.inventory.domain.analytics.controller.dto.response.MenuSalesDetailResponse;
import kr.inventory.domain.analytics.controller.dto.response.RefundSummaryResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesPeakResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesSummaryResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesTrendResponse;
import kr.inventory.domain.analytics.service.SalesAnalyticsService;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderDetailResponse;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderSummaryResponse;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerTotalSummaryResponse;
import kr.inventory.domain.sales.service.SalesLedgerService;
import kr.inventory.domain.sales.service.command.SalesLedgerQueryCondition;
import kr.inventory.global.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesAiQueryService {

    private final SalesAnalyticsService salesAnalyticsService;
    private final SalesLedgerService salesLedgerService;
    private final SalesSuggestedActionProvider salesSuggestedActionProvider;

    public SalesSummaryToolResponse getSalesSummary(Long userId, UUID storePublicId, SalesSummaryToolRequest request) {
        SalesToolDateRange currentRange = SalesToolDateRangeResolver.resolve(
                request.period(),
                request.fromDate(),
                request.toDate(),
                DateRangePreset.LAST_7_DAYS
        );
        String interval = request.normalizedInterval();
        String compareMode = request.normalizedCompareMode();

        SalesToolDateRange baseRange = switch (compareMode) {
            case "previous_period" -> {
                long days = currentRange.toDate().toEpochDay() - currentRange.fromDate().toEpochDay() + 1;
                LocalDate baseTo = currentRange.fromDate().minusDays(1);
                LocalDate baseFrom = baseTo.minusDays(days - 1);
                yield new SalesToolDateRange(
                        "previous_period",
                        baseFrom,
                        baseTo,
                        baseFrom.atStartOfDay(SalesConstants.KST).toOffsetDateTime(),
                        baseTo.atTime(23, 59, 59).atZone(SalesConstants.KST).toOffsetDateTime()
                );
            }
            case "same_period_last_week" -> {
                LocalDate baseFrom = currentRange.fromDate().minusWeeks(1);
                LocalDate baseTo = currentRange.toDate().minusWeeks(1);
                yield new SalesToolDateRange(
                        "same_period_last_week",
                        baseFrom,
                        baseTo,
                        baseFrom.atStartOfDay(SalesConstants.KST).toOffsetDateTime(),
                        baseTo.atTime(23, 59, 59).atZone(SalesConstants.KST).toOffsetDateTime()
                );
            }
            case "same_period_last_month" -> {
                LocalDate baseFrom = currentRange.fromDate().minusMonths(1);
                LocalDate baseTo = currentRange.toDate().minusMonths(1);
                yield new SalesToolDateRange(
                        "same_period_last_month",
                        baseFrom,
                        baseTo,
                        baseFrom.atStartOfDay(SalesConstants.KST).toOffsetDateTime(),
                        baseTo.atTime(23, 59, 59).atZone(SalesConstants.KST).toOffsetDateTime()
                );
            }
            case "custom" -> {
                LocalDate baseFromDate = request.baseFromDate();
                LocalDate baseToDate = request.baseToDate();
                if (baseFromDate == null || baseToDate == null) {
                    throw new SalesException(SalesErrorCode.BOTH_DATES_REQUIRED);
                }
                if (baseFromDate.isAfter(baseToDate)) {
                    throw new SalesException(SalesErrorCode.INVALID_DATE_RANGE);
                }
                yield new SalesToolDateRange(
                        "custom",
                        baseFromDate,
                        baseToDate,
                        baseFromDate.atStartOfDay(SalesConstants.KST).toOffsetDateTime(),
                        baseToDate.atTime(23, 59, 59).atZone(SalesConstants.KST).toOffsetDateTime()
                );
            }
            default -> throw new IllegalArgumentException("Unsupported compareMode: " + compareMode);
        };

        SalesSummaryResponse current = salesAnalyticsService.getSalesSummarySnapshot(
                userId,
                storePublicId,
                currentRange.fromDateTime(),
                currentRange.toDateTime()
        );
        SalesSummaryResponse base = salesAnalyticsService.getSalesSummarySnapshot(
                userId,
                storePublicId,
                baseRange.fromDateTime(),
                baseRange.toDateTime()
        );

        long currentOrderCount = current.totalOrderCount();
        long baseOrderCount = base.totalOrderCount();
        Double orderCountGrowthRate = baseOrderCount == 0L
                ? (currentOrderCount > 0L ? 100.0 : 0.0)
                : ((double) (currentOrderCount - baseOrderCount) / baseOrderCount) * 100.0;

        BigDecimal currentTotalAmount = current.totalAmount() != null ? current.totalAmount() : BigDecimal.ZERO;
        BigDecimal baseTotalAmount = base.totalAmount();
        Double totalAmountGrowthRate = baseTotalAmount == null || baseTotalAmount.compareTo(BigDecimal.ZERO) == 0
                ? (currentTotalAmount.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0)
                : currentTotalAmount.subtract(baseTotalAmount)
                        .divide(baseTotalAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

        BigDecimal currentAvgAmount = current.averageOrderAmount() != null ? current.averageOrderAmount() : BigDecimal.ZERO;
        BigDecimal baseAvgAmount = base.averageOrderAmount();
        Double avgAmountGrowthRate = baseAvgAmount == null || baseAvgAmount.compareTo(BigDecimal.ZERO) == 0
                ? (currentAvgAmount.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0)
                : currentAvgAmount.subtract(baseAvgAmount)
                        .divide(baseAvgAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

        BigDecimal currentMaxAmount = current.maxOrderAmount() != null ? current.maxOrderAmount() : BigDecimal.ZERO;
        BigDecimal baseMaxAmount = base.maxOrderAmount();
        Double maxAmountGrowthRate = baseMaxAmount == null || baseMaxAmount.compareTo(BigDecimal.ZERO) == 0
                ? (currentMaxAmount.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0)
                : currentMaxAmount.subtract(baseMaxAmount)
                        .divide(baseMaxAmount, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

        return SalesSummaryToolResponse.from(
                currentRange,
                baseRange,
                interval,
                compareMode,
                current,
                base,
                orderCountGrowthRate,
                totalAmountGrowthRate,
                avgAmountGrowthRate,
                maxAmountGrowthRate,
                salesSuggestedActionProvider.buildSummaryFollowUps(currentRange)
        );
    }

    public SalesTrendToolResponse getSalesTrend(Long userId, UUID storePublicId, SalesTrendToolRequest request) {
        SalesToolDateRange range = SalesToolDateRangeResolver.resolve(
                request.period(),
                request.fromDate(),
                request.toDate(),
                DateRangePreset.LAST_7_DAYS
        );
        String interval = request.normalizedInterval();
        String metric = request.normalizedMetric();

        List<SalesTrendPointToolResponse> trend = salesAnalyticsService.getSalesTrend(
                        userId,
                        storePublicId,
                        range.fromDateTime(),
                        range.toDateTime(),
                        interval
                ).stream()
                .map(SalesTrendPointToolResponse::from)
                .toList();

        SalesTrendPointToolResponse highestPoint = null;
        if (!trend.isEmpty()) {
            ToDoubleFunction<SalesTrendPointToolResponse> metricExtractorHigh = switch (metric) {
                case "order_count" -> point -> point.orderCount();
                default -> point -> point.totalAmount() != null ? point.totalAmount().doubleValue() : 0.0;
            };
            Comparator<SalesTrendPointToolResponse> comparatorHigh = Comparator.comparingDouble(metricExtractorHigh)
                    .reversed()
                    .thenComparing(SalesTrendPointToolResponse::date);
            highestPoint = trend.stream().sorted(comparatorHigh).findFirst().orElse(null);
        }

        SalesTrendPointToolResponse lowestPoint = null;
        if (!trend.isEmpty()) {
            ToDoubleFunction<SalesTrendPointToolResponse> metricExtractorLow = switch (metric) {
                case "order_count" -> point -> point.orderCount();
                default -> point -> point.totalAmount() != null ? point.totalAmount().doubleValue() : 0.0;
            };
            Comparator<SalesTrendPointToolResponse> comparatorLow = Comparator.comparingDouble(metricExtractorLow)
                    .thenComparing(SalesTrendPointToolResponse::date);
            lowestPoint = trend.stream().sorted(comparatorLow).findFirst().orElse(null);
        }

        SalesTrendPointToolResponse latestPoint = trend.isEmpty() ? null : trend.get(trend.size() - 1);

        Double overallChangeRate = null;
        if (trend.size() >= 2) {
            SalesTrendPointToolResponse first = trend.get(0);
            SalesTrendPointToolResponse last = trend.get(trend.size() - 1);

            if ("order_count".equals(metric)) {
                long currentCount = last.orderCount();
                long previousCount = first.orderCount();
                overallChangeRate = previousCount == 0L
                        ? (currentCount > 0L ? 100.0 : 0.0)
                        : ((double) (currentCount - previousCount) / previousCount) * 100.0;
            } else {
                BigDecimal currentAmount = last.totalAmount() != null ? last.totalAmount() : BigDecimal.ZERO;
                BigDecimal previousAmount = first.totalAmount() != null ? first.totalAmount() : BigDecimal.ZERO;
                overallChangeRate = previousAmount.compareTo(BigDecimal.ZERO) == 0
                        ? (currentAmount.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0)
                        : currentAmount.subtract(previousAmount)
                                .divide(previousAmount, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .doubleValue();
            }
        }

        return SalesTrendToolResponse.from(
                range,
                interval,
                metric,
                trend,
                highestPoint,
                lowestPoint,
                latestPoint,
                overallChangeRate,
                salesSuggestedActionProvider.buildTrendFollowUps(range)
        );
    }

    public SalesPeakToolResponse getSalesPeak(Long userId, UUID storePublicId, SalesPeakToolRequest request) {
        SalesToolDateRange range = SalesToolDateRangeResolver.resolve(
                request.period(),
                request.fromDate(),
                request.toDate(),
                DateRangePreset.LAST_7_DAYS
        );
        int limit = request.resolvedLimit();
        String viewType = request.normalizedViewType();

        List<SalesPeakResponse> rawPeaks = salesAnalyticsService.getSalesPeak(
                userId,
                storePublicId,
                range.fromDateTime(),
                range.toDateTime()
        );

        List<SalesPeakItemToolResponse> topTimeSlots = rawPeaks.stream()
                .sorted(Comparator
                        .comparingLong(SalesPeakResponse::orderCount).reversed()
                        .thenComparingInt(SalesPeakResponse::dayOfWeek)
                        .thenComparingInt(SalesPeakResponse::hour))
                .limit(limit)
                .map(SalesPeakItemToolResponse::from)
                .toList();

        List<SalesPeakDayToolResponse> topDays = rawPeaks.stream()
                .collect(Collectors.groupingBy(
                        SalesPeakResponse::dayOfWeek,
                        Collectors.summingLong(SalesPeakResponse::orderCount)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(entry -> SalesPeakDayToolResponse.from(entry.getKey(), entry.getValue()))
                .toList();

        List<SalesPeakHourToolResponse> topHours = rawPeaks.stream()
                .collect(Collectors.groupingBy(
                        SalesPeakResponse::hour,
                        Collectors.summingLong(SalesPeakResponse::orderCount)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(entry -> SalesPeakHourToolResponse.from(entry.getKey(), entry.getValue()))
                .toList();

        List<SalesPeakItemToolResponse> responseTimeSlots = "combined".equals(viewType) ? topTimeSlots : List.of();
        List<SalesPeakDayToolResponse> responseDays = "hour_only".equals(viewType) ? List.of() : topDays;
        List<SalesPeakHourToolResponse> responseHours = "day_only".equals(viewType) ? List.of() : topHours;

        return SalesPeakToolResponse.from(
                range,
                viewType,
                responseTimeSlots,
                responseDays,
                responseHours,
                topDays.isEmpty() ? null : topDays.get(0),
                topHours.isEmpty() ? null : topHours.get(0),
                salesSuggestedActionProvider.buildPeakFollowUps(range)
        );
    }

    public TopMenuRankingToolResponse getTopMenuRanking(Long userId, UUID storePublicId, TopMenuRankingToolRequest request) {
        SalesToolDateRange range = SalesToolDateRangeResolver.resolve(
                request.period(),
                request.fromDate(),
                request.toDate(),
                DateRangePreset.LAST_7_DAYS
        );
        int topN = request.resolvedTopN();
        String rankBy = request.normalizedRankBy();

        List<MenuRankingResponse> ranking = salesAnalyticsService.getMenuRanking(
                userId,
                storePublicId,
                range.fromDateTime(),
                range.toDateTime(),
                topN,
                rankBy
        );

        SalesSummaryResponse summary = salesAnalyticsService.getSalesSummarySnapshot(
                userId,
                storePublicId,
                range.fromDateTime(),
                range.toDateTime()
        );
        BigDecimal totalSalesAmount = summary.totalAmount();

        List<TopMenuRankingItemToolResponse> menus = ranking.stream()
                .map(item -> TopMenuRankingItemToolResponse.from(item, totalSalesAmount))
                .toList();

        return TopMenuRankingToolResponse.from(
                range,
                rankBy,
                totalSalesAmount,
                menus,
                salesSuggestedActionProvider.buildTopMenuFollowUps(range, rankBy)
        );
    }

    public SalesRecordsToolResponse getSalesRecords(Long userId, UUID storePublicId, SalesRecordsToolRequest request) {
        BigDecimal amountMin = request.resolvedAmountMin();
        BigDecimal amountMax = request.resolvedAmountMax();
        if (amountMin != null && amountMax != null && amountMin.compareTo(amountMax) > 0) {
            throw new SalesException(SalesErrorCode.INVALID_AMOUNT_RANGE);
        }

        SalesToolDateRange range = request.resolvedDateRange();
        SalesLedgerQueryCondition condition = new SalesLedgerQueryCondition(
                range.fromDateTime(),
                range.toDateTime(),
                request.resolvedStatus(),
                request.resolvedType(),
                request.normalizedMenuName(),
                request.resolvedAmountMin(),
                request.resolvedAmountMax(),
                request.normalizedTableCode(),
                request.resolvedSortBy()
        );

        PageResponse<SalesLedgerOrderSummaryResponse> orderPage = salesLedgerService.getSalesLedgerOrders(
                userId,
                storePublicId,
                condition,
                PageRequest.of(request.resolvedPageIndex(), request.resolvedSize())
        );
        SalesLedgerTotalSummaryResponse summary = salesLedgerService.getSalesLedgerTotalSummary(
                userId,
                storePublicId,
                condition
        );

        List<SalesRecordItemToolResponse> orders = orderPage.content().stream()
                .map(SalesRecordItemToolResponse::from)
                .toList();

        return SalesRecordsToolResponse.from(
                range,
                request.normalizedStatus(),
                request.normalizedType(),
                SalesRecordsSummaryToolResponse.from(summary),
                orders,
                SalesRecordsPageInfoToolResponse.from(orderPage),
                salesSuggestedActionProvider.buildRecordsFollowUps(range, orders)
        );
    }

    public SalesOrderDetailToolResponse getSalesOrderDetail(Long userId, UUID storePublicId, SalesOrderDetailToolRequest request) {
        BigDecimal amountMin = request.resolvedAmountMin();
        BigDecimal amountMax = request.resolvedAmountMax();
        if (amountMin != null && amountMax != null && amountMin.compareTo(amountMax) > 0) {
            throw new SalesException(SalesErrorCode.INVALID_AMOUNT_RANGE);
        }

        UUID targetOrderPublicId = request.orderPublicId();

        if (targetOrderPublicId == null) {
            if (!request.hasDirectOrderPublicId() && !request.hasLookupCondition()) {
                throw new SalesException(SalesErrorCode.ORDER_DETAIL_LOOKUP_REQUIRES_CONDITION);
            }
            SalesToolDateRange range = request.resolvedDateRange();
            SalesLedgerQueryCondition condition = new SalesLedgerQueryCondition(
                    range.fromDateTime(),
                    range.toDateTime(),
                    request.resolvedStatus(),
                    request.resolvedType(),
                    request.normalizedMenuName(),
                    request.resolvedAmountMin(),
                    request.resolvedAmountMax(),
                    request.normalizedTableCode(),
                    request.resolvedSortBy()
            );

            PageResponse<SalesLedgerOrderSummaryResponse> lookupPage = salesLedgerService.getSalesLedgerOrders(
                    userId,
                    storePublicId,
                    condition,
                    PageRequest.of(request.resolvedSelectionIndex() - 1, 1)
            );

            if (lookupPage.content().isEmpty()) {
                throw new SalesException(SalesErrorCode.SALES_RECORD_NOT_FOUND);
            }

            targetOrderPublicId = lookupPage.content().get(0).orderPublicId();
        }

        SalesLedgerOrderDetailResponse detail = salesLedgerService.getSalesLedgerOrder(
                userId,
                storePublicId,
                targetOrderPublicId
        );

        return SalesOrderDetailToolResponse.from(
                detail,
                salesSuggestedActionProvider.buildOrderDetailFollowUps(detail)
        );
    }

    public SalesRefundSummaryToolResponse getRefundSummary(Long userId, UUID storePublicId, SalesRefundSummaryToolRequest request) {
        SalesToolDateRange range = SalesToolDateRangeResolver.resolve(
                request.period(),
                request.fromDate(),
                request.toDate(),
                DateRangePreset.LAST_7_DAYS
        );

        RefundSummaryResponse response = salesAnalyticsService.getRefundSummary(
                userId,
                storePublicId,
                range.fromDateTime(),
                range.toDateTime()
        );

        return SalesRefundSummaryToolResponse.from(
                range,
                response,
                salesSuggestedActionProvider.buildRefundFollowUps(range)
        );
    }

    public MenuSalesDetailToolResponse getMenuSalesDetail(Long userId, UUID storePublicId, MenuSalesDetailToolRequest request) {
        SalesToolDateRange range = SalesToolDateRangeResolver.resolve(
                request.period(),
                request.fromDate(),
                request.toDate(),
                DateRangePreset.LAST_7_DAYS
        );
        String menuName = request.normalizedMenuName();
        if (menuName == null) {
            throw new SalesException(SalesErrorCode.INVALID_MENU_NAME);
        }

        MenuSalesDetailResponse response = salesAnalyticsService.getMenuSalesDetail(
                userId,
                storePublicId,
                range.fromDateTime(),
                range.toDateTime(),
                menuName
        );

        return MenuSalesDetailToolResponse.from(
                range,
                response,
                salesSuggestedActionProvider.buildMenuDetailFollowUps(range, response.menuName())
        );
    }

}
