package kr.inventory.domain.analytics.service;

import kr.inventory.domain.analytics.constant.SalesAnalyticsConstants;
import kr.inventory.domain.analytics.controller.dto.response.*;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.repository.SalesOrderSearchRepositoryCustom;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAnalyticsService {

    private final StoreAccessValidator storeAccessValidator;
    private final SalesOrderSearchRepositoryCustom salesOrderSearchRepository;

    @Cacheable(
            value = "sales:trend",
            key = "#storePublicId + ':' + (#from == null ? 'null' : #from.toInstant().toEpochMilli()) + ':' + (#to == null ? 'null' : #to.toInstant().toEpochMilli()) + ':' + #interval",
            condition = "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
    )
    public List<SalesTrendResponse> getSalesTrend(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to,
            String interval
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();
        String validInterval = normalizeInterval(interval);

        validTo = validateAndAdjustDateRange(validFrom, validTo);

        log.debug("[Analytics] 매출 추이 집계 storeId={} interval={}", storeId, validInterval);
        return salesOrderSearchRepository.aggregateSalesTrend(storeId, validFrom, validTo, validInterval);
    }

    @Cacheable(
            value = "sales:peak",
            key = "#storePublicId + ':' + (#from == null ? 'null' : #from.toInstant().toEpochMilli()) + ':' + (#to == null ? 'null' : #to.toInstant().toEpochMilli())",
            condition = "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
    )
    public List<SalesPeakResponse> getSalesPeak(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();

        validTo = validateAndAdjustDateRange(validFrom, validTo);

        log.debug("[Analytics] 피크 집계 storeId={}", storeId);
        return salesOrderSearchRepository.aggregateSalesPeak(storeId, validFrom, validTo);
    }

    @Cacheable(
            value = "sales:menu-ranking",
            key = "#storePublicId + ':' + (#from == null ? 'null' : #from.toInstant().toEpochMilli()) + ':' + (#to == null ? 'null' : #to.toInstant().toEpochMilli()) + ':' + #topN + ':' + (#rankBy == null ? 'quantity' : #rankBy)",
            condition = "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
    )
    public List<MenuRankingResponse> getMenuRanking(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to,
            Integer topN,
            String rankBy
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();
        int validTopN = (topN != null) ? topN : SalesAnalyticsConstants.DEFAULT_TOP_N;
        String validRankBy = normalizeRankBy(rankBy);

        validTo = validateAndAdjustDateRange(validFrom, validTo);
        validateTopN(validTopN);

        log.debug("[Analytics] 메뉴 랭킹 집계 storeId={} topN={} rankBy={}", storeId, validTopN, validRankBy);
        return salesOrderSearchRepository.aggregateMenuRanking(storeId, validFrom, validTo, validTopN, validRankBy);
    }

    public List<MenuRankingResponse> getMenuRanking(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to,
            Integer topN
    ) {
        return getMenuRanking(userId, storePublicId, from, to, topN, "quantity");
    }

    @Cacheable(
            value = "sales:summary",
            key = "#storePublicId + ':' + (#from == null ? 'null' : #from.toInstant().toEpochMilli()) + ':' + (#to == null ? 'null' : #to.toInstant().toEpochMilli()) + ':' + #interval",
            condition = "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
    )
    public SalesSummaryResponse getSalesSummary(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to,
            String interval
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();
        String validInterval = normalizeInterval(interval);

        SalesSummaryResponse current = getSalesSummarySnapshot(userId, storePublicId, validFrom, validTo);
        OffsetDateTime adjustedTo = validateAndAdjustDateRange(validFrom, validTo);
        OffsetDateTime[] previousPeriod = calculatePreviousPeriod(validFrom, adjustedTo, validInterval);
        SalesSummaryResponse previous = getSalesSummarySnapshot(userId, storePublicId, previousPeriod[0], previousPeriod[1]);

        return new SalesSummaryResponse(
                current.totalOrderCount(),
                current.totalAmount(),
                current.averageOrderAmount(),
                current.maxOrderAmount(),
                current.minOrderAmount(),
                calculateGrowthRate(current.totalOrderCount(), previous.totalOrderCount()),
                calculateGrowthRate(current.totalAmount(), previous.totalAmount()),
                calculateGrowthRate(current.averageOrderAmount(), previous.averageOrderAmount()),
                calculateGrowthRate(current.maxOrderAmount(), previous.maxOrderAmount())
        );
    }

    /**
     * 환불 요약
     */
    @Cacheable(
            value = "sales:refund-summary",
            key = "#storePublicId + ':' + #from.toInstant().toEpochMilli() + ':' + #to.toInstant().toEpochMilli()",
            condition = "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
    )
    public RefundSummaryResponse getRefundSummary(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();
        validTo = validateAndAdjustDateRange(validFrom, validTo);

        log.debug("[Analytics] 환불 요약 집계 storeId={}", storeId);
        return salesOrderSearchRepository.aggregateRefundSummary(storeId, validFrom, validTo);
    }

    /**
     * 특정 메뉴 상세 집계
     */
    @Cacheable(
            value = "sales:menu-detail",
            key = "#storePublicId + ':' + #from.toInstant().toEpochMilli() + ':' + #to.toInstant().toEpochMilli() + ':' + #menuName",
            condition = "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
    )
    public MenuSalesDetailResponse getMenuSalesDetail(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to,
            String menuName
    ) {
        // 1. menuName 검증 (빠른 실패)
        if (menuName == null || menuName.isBlank()) {
            throw new AnalyticsException(AnalyticsErrorCode.INVALID_MENU_NAME);
        }
        String trimmedMenuName = menuName.trim();

        // 2. 권한 검증
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        // 3. 날짜 기본값 + 검증
        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();
        validTo = validateAndAdjustDateRange(validFrom, validTo);

        log.debug("[Analytics] 메뉴 상세 집계 storeId={} menuName={}", storeId, trimmedMenuName);
        return salesOrderSearchRepository.aggregateMenuSalesDetail(storeId, validFrom, validTo, trimmedMenuName);
    }


    public SalesSummaryResponse getSalesSummarySnapshot(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();
        OffsetDateTime adjustedTo = validateAndAdjustDateRange(validFrom, validTo);

        log.debug("[Analytics] 매출 스냅샷 집계 storeId={}", storeId);
        return salesOrderSearchRepository.aggregateSalesSummary(storeId, validFrom, adjustedTo);
    }

    private OffsetDateTime[] calculatePreviousPeriod(OffsetDateTime from, OffsetDateTime to, String interval) {
        long daysDiff = Duration.between(from, to).toDays() + 1;

        return switch (interval) {
            case "Month" -> new OffsetDateTime[]{from.minusMonths(1), to.minusMonths(1)};
            case "Week" -> new OffsetDateTime[]{from.minusWeeks(1), to.minusWeeks(1)};
            default -> new OffsetDateTime[]{from.minusDays(daysDiff), to.minusDays(daysDiff)};
        };
    }

    private Double calculateGrowthRate(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) (current - previous) / previous) * 100.0;
    }

    private Double calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return (current != null && current.compareTo(BigDecimal.ZERO) > 0) ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private String normalizeInterval(String interval) {
        if (interval == null) {
            return "Day";
        }

        return switch (interval.toLowerCase(Locale.ROOT)) {
            case "day" -> "Day";
            case "week" -> "Week";
            case "month" -> "Month";
            case "hour"  -> "Hour";
            default -> throw new AnalyticsException(AnalyticsErrorCode.INVALID_INTERVAL);
        };
    }

    private String normalizeRankBy(String rankBy) {
        if (rankBy == null || rankBy.trim().isEmpty()) {
            return "quantity";
        }

        String normalized = rankBy.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "quantity", "amount" -> normalized;
            default -> throw new IllegalArgumentException("rankBy must be either quantity or amount.");
        };
    }

    private OffsetDateTime validateAndAdjustDateRange(OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime now = OffsetDateTime.now();

        if (from.isAfter(now)) {
            throw new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }

        OffsetDateTime adjustedTo = to.isAfter(now) ? now : to;

        if (from.isAfter(adjustedTo)) {
            throw new AnalyticsException(AnalyticsErrorCode.INVALID_DATE_RANGE);
        }

        long daysBetween = Duration.between(from, adjustedTo).toDays();
        if (daysBetween > SalesAnalyticsConstants.MAX_QUERY_DAYS) {
            throw new AnalyticsException(AnalyticsErrorCode.DATE_RANGE_TOO_LONG);
        }

        return adjustedTo;
    }

    private void validateTopN(int topN) {
        if (topN < SalesAnalyticsConstants.MIN_TOP_N || topN > SalesAnalyticsConstants.MAX_TOP_N) {
            throw new AnalyticsException(AnalyticsErrorCode.INVALID_TOP_N);
        }
    }
}
