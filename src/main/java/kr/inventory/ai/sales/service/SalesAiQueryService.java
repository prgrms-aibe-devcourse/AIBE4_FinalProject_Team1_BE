package kr.inventory.ai.sales.service;

import kr.inventory.ai.sales.constant.SalesConstants;
import kr.inventory.ai.sales.exception.SalesErrorCode;
import kr.inventory.ai.sales.exception.SalesException;
import kr.inventory.ai.sales.tool.dto.response.*;
import kr.inventory.domain.analytics.controller.dto.response.MenuRankingResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesPeakResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesSummaryResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesTrendResponse;
import kr.inventory.domain.analytics.service.SalesAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
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

    public SalesSummaryToolResponse getSalesSummary(
            Long userId,
            UUID storePublicId,
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to,
            String interval,
            String compareMode,
            String periodKey,
            String preset,
            java.time.OffsetDateTime customBaseFrom,
            java.time.OffsetDateTime customBaseTo
    ) {
        validateInterval(interval);
        validateCompareMode(compareMode);

        LocalDate currentFrom = from != null ? from.toLocalDate() : null;
        LocalDate currentTo = to != null ? to.toLocalDate() : null;
        LocalDate providedBaseFrom = customBaseFrom != null ? customBaseFrom.toLocalDate() : null;
        LocalDate providedBaseTo = customBaseTo != null ? customBaseTo.toLocalDate() : null;

        LocalDate[] baseRange = resolveBaseRange(currentFrom, currentTo, compareMode, providedBaseFrom, providedBaseTo);
        LocalDate baseFrom = baseRange[0];
        LocalDate baseTo = baseRange[1];

        SalesSummaryResponse current = salesAnalyticsService.getSalesSummarySnapshot(
                userId,
                storePublicId,
                from,
                to
        );
        SalesSummaryResponse base = salesAnalyticsService.getSalesSummarySnapshot(
                userId,
                storePublicId,
                baseFrom.atStartOfDay(SalesConstants.KST).toOffsetDateTime(),
                baseTo.atTime(23, 59, 59).atZone(SalesConstants.KST).toOffsetDateTime()
        );

        return new SalesSummaryToolResponse(
                "sales.summary",
                periodKey,
                preset,
                currentFrom,
                currentTo,
                interval,
                compareMode,
                baseFrom,
                baseTo,
                current.totalOrderCount(),
                current.totalAmount(),
                current.averageOrderAmount(),
                current.maxOrderAmount(),
                current.minOrderAmount(),
                calculateGrowthRate(current.totalOrderCount(), base.totalOrderCount()),
                calculateGrowthRate(current.totalAmount(), base.totalAmount()),
                calculateGrowthRate(current.averageOrderAmount(), base.averageOrderAmount()),
                calculateGrowthRate(current.maxOrderAmount(), base.maxOrderAmount()),
                buildSuggestedFollowUps("sales.summary", periodKey)
        );
    }

    public SalesTrendToolResponse getSalesTrend(
            Long userId,
            UUID storePublicId,
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to,
            String interval,
            String metric,
            String periodKey,
            String preset
    ) {
        validateInterval(interval);
        validateMetric(metric);

        List<SalesTrendPointToolResponse> trend = salesAnalyticsService.getSalesTrend(
                        userId,
                        storePublicId,
                        from,
                        to,
                        interval
                ).stream()
                .map(this::toTrendPoint)
                .toList();

        SalesTrendPointToolResponse highestPoint = selectExtremePoint(trend, metric, true);
        SalesTrendPointToolResponse lowestPoint = selectExtremePoint(trend, metric, false);
        SalesTrendPointToolResponse latestPoint = trend.isEmpty() ? null : trend.get(trend.size() - 1);
        Double overallChangeRate = calculateOverallChangeRate(trend, metric);

        return new SalesTrendToolResponse(
                "sales.trend",
                periodKey,
                preset,
                from != null ? from.toLocalDate() : null,
                to != null ? to.toLocalDate() : null,
                interval,
                metric,
                trend.size(),
                highestPoint,
                lowestPoint,
                latestPoint,
                overallChangeRate,
                trend,
                buildSuggestedFollowUps("sales.trend", periodKey)
        );
    }

    public SalesPeakToolResponse getSalesPeak(
            Long userId,
            UUID storePublicId,
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to,
            int limit,
            String viewType,
            String periodKey,
            String preset
    ) {
        validateViewType(viewType);

        List<SalesPeakResponse> rawPeaks = salesAnalyticsService.getSalesPeak(
                userId,
                storePublicId,
                from,
                to
        );

        List<SalesPeakItemToolResponse> topTimeSlots = rawPeaks.stream()
                .sorted(Comparator
                        .comparingLong(SalesPeakResponse::orderCount).reversed()
                        .thenComparingInt(SalesPeakResponse::dayOfWeek)
                        .thenComparingInt(SalesPeakResponse::hour))
                .limit(limit)
                .map(this::toPeakItem)
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
                .map(entry -> new SalesPeakDayToolResponse(
                        entry.getKey(),
                        toDayLabel(entry.getKey()),
                        entry.getValue()
                ))
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
                .map(entry -> new SalesPeakHourToolResponse(
                        entry.getKey(),
                        toTimeRange(entry.getKey()),
                        entry.getValue()
                ))
                .toList();

        List<SalesPeakItemToolResponse> responseTimeSlots = "combined".equals(viewType) ? topTimeSlots : List.of();
        List<SalesPeakDayToolResponse> responseDays = "hour_only".equals(viewType) ? List.of() : topDays;
        List<SalesPeakHourToolResponse> responseHours = "day_only".equals(viewType) ? List.of() : topHours;

        return new SalesPeakToolResponse(
                "sales.peak",
                periodKey,
                preset,
                from != null ? from.toLocalDate() : null,
                to != null ? to.toLocalDate() : null,
                viewType,
                responseTimeSlots.size(),
                responseTimeSlots,
                responseDays.size(),
                responseDays,
                responseHours.size(),
                responseHours,
                topDays.isEmpty() ? null : topDays.get(0),
                topHours.isEmpty() ? null : topHours.get(0),
                buildSuggestedFollowUps("sales.peak", periodKey)
        );
    }

    public TopMenuRankingToolResponse getTopMenuRanking(
            Long userId,
            UUID storePublicId,
            java.time.OffsetDateTime from,
            java.time.OffsetDateTime to,
            int topN,
            String rankBy,
            String periodKey,
            String preset
    ) {
        validateRankBy(rankBy);

        List<MenuRankingResponse> ranking = salesAnalyticsService.getMenuRanking(
                userId,
                storePublicId,
                from,
                to,
                topN,
                rankBy
        );

        SalesSummaryResponse summary = salesAnalyticsService.getSalesSummarySnapshot(
                userId,
                storePublicId,
                from,
                to
        );
        BigDecimal totalSalesAmount = summary.totalAmount();

        List<TopMenuRankingItemToolResponse> menus = ranking.stream()
                .map(item -> toMenuItem(item, totalSalesAmount))
                .toList();

        return new TopMenuRankingToolResponse(
                "sales.top_menu",
                periodKey,
                preset,
                from != null ? from.toLocalDate() : null,
                to != null ? to.toLocalDate() : null,
                rankBy,
                totalSalesAmount,
                menus.size(),
                menus,
                buildSuggestedFollowUps("sales.top_menu", periodKey)
        );
    }

    private LocalDate[] resolveBaseRange(
            LocalDate currentFrom,
            LocalDate currentTo,
            String compareMode,
            LocalDate providedBaseFrom,
            LocalDate providedBaseTo
    ) {
        return switch (compareMode) {
            case "previous_period" -> {
                long days = currentTo.toEpochDay() - currentFrom.toEpochDay() + 1;
                LocalDate baseTo = currentFrom.minusDays(1);
                LocalDate baseFrom = baseTo.minusDays(days - 1);
                yield new LocalDate[]{baseFrom, baseTo};
            }
            case "same_period_last_week" -> new LocalDate[]{
                    currentFrom.minusWeeks(1),
                    currentTo.minusWeeks(1)
            };
            case "same_period_last_month" -> new LocalDate[]{
                    currentFrom.minusMonths(1),
                    currentTo.minusMonths(1)
            };
            case "custom" -> {
                if (providedBaseFrom == null || providedBaseTo == null) {
                    throw new SalesException(SalesErrorCode.UNSUPPORTED_COMPARE_MODE);
                }
                yield new LocalDate[]{providedBaseFrom, providedBaseTo};
            }
            default -> throw new SalesException(SalesErrorCode.UNSUPPORTED_COMPARE_MODE);
        };
    }


    private Double calculateGrowthRate(long current, long previous) {
        if (previous == 0L) {
            return current > 0L ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100.0;
    }

    private Double calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = current != null ? current : BigDecimal.ZERO;
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return safeCurrent.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private SalesTrendPointToolResponse selectExtremePoint(
            List<SalesTrendPointToolResponse> trend,
            String metric,
            boolean highest
    ) {
        if (trend.isEmpty()) {
            return null;
        }

        ToDoubleFunction<SalesTrendPointToolResponse> metricExtractor = switch (metric) {
            case "order_count" -> point -> point.orderCount();
            default -> point -> point.totalAmount() != null ? point.totalAmount().doubleValue() : 0.0;
        };

        Comparator<SalesTrendPointToolResponse> comparator = Comparator.comparingDouble(metricExtractor);
        if (highest) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(SalesTrendPointToolResponse::date);

        return trend.stream().sorted(comparator).findFirst().orElse(null);
    }

    private Double calculateOverallChangeRate(List<SalesTrendPointToolResponse> trend, String metric) {
        if (trend.size() < 2) {
            return null;
        }

        SalesTrendPointToolResponse first = trend.get(0);
        SalesTrendPointToolResponse last = trend.get(trend.size() - 1);

        if ("order_count".equals(metric)) {
            return calculateGrowthRate(last.orderCount(), first.orderCount());
        }

        return calculateGrowthRate(
                last.totalAmount() != null ? last.totalAmount() : BigDecimal.ZERO,
                first.totalAmount() != null ? first.totalAmount() : BigDecimal.ZERO
        );
    }

    private SalesTrendPointToolResponse toTrendPoint(SalesTrendResponse response) {
        return new SalesTrendPointToolResponse(
                response.date(),
                response.orderCount(),
                response.totalAmount()
        );
    }

    private SalesPeakItemToolResponse toPeakItem(SalesPeakResponse response) {
        return new SalesPeakItemToolResponse(
                response.dayOfWeek(),
                toDayLabel(response.dayOfWeek()),
                response.hour(),
                toTimeRange(response.hour()),
                response.orderCount()
        );
    }

    private TopMenuRankingItemToolResponse toMenuItem(MenuRankingResponse response, BigDecimal totalSalesAmount) {
        return new TopMenuRankingItemToolResponse(
                response.rank(),
                response.menuName(),
                response.totalQuantity(),
                response.totalAmount(),
                calculateAmountShareRate(response.totalAmount(), totalSalesAmount)
        );
    }

    private BigDecimal calculateAmountShareRate(BigDecimal menuAmount, BigDecimal totalSalesAmount) {
        if (menuAmount == null || totalSalesAmount == null || totalSalesAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return menuAmount
                .divide(totalSalesAmount, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String toDayLabel(int dayOfWeek) {
        return DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, SalesConstants.DAY_LABEL_LOCALE);
    }

    private String toTimeRange(int hour) {
        return String.format("%02d:00-%02d:59", hour, hour);
    }

    private List<kr.inventory.ai.sales.tool.dto.response.SuggestedAction> buildSuggestedFollowUps(
            String currentActionKey,
            String currentPeriodKey
    ) {
        List<kr.inventory.ai.sales.tool.dto.response.SuggestedAction> suggestions = new java.util.ArrayList<>();

        switch (currentActionKey) {
            case "sales.summary" -> {
                suggestions.add(new kr.inventory.ai.sales.tool.dto.response.SuggestedAction(
                        "sales.trend", "매출 추이 보기", currentPeriodKey
                ));
                suggestions.add(new kr.inventory.ai.sales.tool.dto.response.SuggestedAction(
                        "sales.top_menu", "상위 메뉴 순위", currentPeriodKey
                ));
                if (!"today".equals(currentPeriodKey)) {
                    suggestions.add(new kr.inventory.ai.sales.tool.dto.response.SuggestedAction(
                            "sales.summary", "오늘 매출 요약", "today"
                    ));
                }
            }
            case "sales.trend" -> {
                suggestions.add(new kr.inventory.ai.sales.tool.dto.response.SuggestedAction(
                        "sales.peak", "피크 시간대 분석", currentPeriodKey
                ));
                suggestions.add(new kr.inventory.ai.sales.tool.dto.response.SuggestedAction(
                        "sales.summary", "매출 요약 보기", currentPeriodKey
                ));
            }
            case "sales.peak" -> {
                suggestions.add(new kr.inventory.ai.sales.tool.dto.response.SuggestedAction(
                        "sales.top_menu", "인기 메뉴 확인", currentPeriodKey
                ));
                suggestions.add(new kr.inventory.ai.sales.tool.dto.response.SuggestedAction(
                        "sales.trend", "매출 추이 보기", currentPeriodKey
                ));
            }
            case "sales.top_menu" -> {
                suggestions.add(new kr.inventory.ai.sales.tool.dto.response.SuggestedAction(
                        "sales.summary", "전체 매출 요약", currentPeriodKey
                ));
                suggestions.add(new kr.inventory.ai.sales.tool.dto.response.SuggestedAction(
                        "sales.peak", "피크 시간대 분석", currentPeriodKey
                ));
            }
        }

        return suggestions;
    }

    private void validateInterval(String interval) {
        if (!"day".equals(interval) && !"week".equals(interval) && !"month".equals(interval)) {
            throw new SalesException(SalesErrorCode.INVALID_INTERVAL);
        }
    }

    private void validateMetric(String metric) {
        if (!"amount".equals(metric) && !"order_count".equals(metric) && !"both".equals(metric)) {
            throw new SalesException(SalesErrorCode.INVALID_METRIC);
        }
    }

    private void validateRankBy(String rankBy) {
        if (!"quantity".equals(rankBy) && !"amount".equals(rankBy)) {
            throw new SalesException(SalesErrorCode.INVALID_RANK_BY);
        }
    }

    private void validateCompareMode(String compareMode) {
        if (!"previous_period".equals(compareMode)
                && !"same_period_last_week".equals(compareMode)
                && !"same_period_last_month".equals(compareMode)
                && !"custom".equals(compareMode)) {
            throw new SalesException(SalesErrorCode.UNSUPPORTED_COMPARE_MODE);
        }
    }

    private void validateViewType(String viewType) {
        if (!"combined".equals(viewType) && !"day_only".equals(viewType) && !"hour_only".equals(viewType)) {
            throw new SalesException(SalesErrorCode.INVALID_VIEW_TYPE);
        }
    }
}
