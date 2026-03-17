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
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesAnalyticsService {

    private final StoreAccessValidator storeAccessValidator;
    private final SalesOrderSearchRepositoryCustom salesOrderSearchRepository;

    /**
     * 일/주/월 매출 추이
     */
    @Cacheable(
            value = "sales:trend",
            key = "#storePublicId + ':' + #from.toInstant().toEpochMilli() + ':' + #to.toInstant().toEpochMilli() + ':' + #interval",
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

        // 기본값 설정
        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();
        String validInterval = normalizeInterval(interval);

        // Validation & Adjustment
        validTo = validateAndAdjustDateRange(validFrom, validTo);

        log.debug("[Analytics] 매출 추이 집계 storeId={} interval={}", storeId, validInterval);
        return salesOrderSearchRepository.aggregateSalesTrend(storeId, validFrom, validTo, validInterval);
    }

    /**
     * 요일×시간대 피크
     */
    @Cacheable(
            value = "sales:peak",
            key = "#storePublicId + ':' + #from.toInstant().toEpochMilli() + ':' + #to.toInstant().toEpochMilli()",
            condition = "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
    )
    public List<SalesPeakResponse> getSalesPeak(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        // 기본값 설정
        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();

        // Validation & Adjustment
        validTo = validateAndAdjustDateRange(validFrom, validTo);

        log.debug("[Analytics] 피크 집계 storeId={}", storeId);
        return salesOrderSearchRepository.aggregateSalesPeak(storeId, validFrom, validTo);
    }

    /**
     * 메뉴 TOP N
     */
    @Cacheable(
            value = "sales:menu-ranking",
            key = "#storePublicId + ':' + #from.toInstant().toEpochMilli() + ':' + #to.toInstant().toEpochMilli() + ':' + #topN",
            condition = "#to != null && #to.isBefore(T(java.time.OffsetDateTime).now().withHour(0).withMinute(0).withSecond(0).withNano(0))"
    )
    public List<MenuRankingResponse> getMenuRanking(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to,
            Integer topN
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        // 기본값 설정
        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();
        int validTopN = (topN != null) ? topN : SalesAnalyticsConstants.DEFAULT_TOP_N;

        // Validation & Adjustment
        validTo = validateAndAdjustDateRange(validFrom, validTo);
        validateTopN(validTopN);

        log.debug("[Analytics] 메뉴 랭킹 집계 storeId={} topN={}", storeId, validTopN);
        return salesOrderSearchRepository.aggregateMenuRanking(storeId, validFrom, validTo, validTopN);
    }

    /**
     * 매출 요약 (객단가 등)
     */
    @Cacheable(
            value = "sales:summary",
            key = "#storePublicId + ':' + #from.toInstant().toEpochMilli() + ':' + #to.toInstant().toEpochMilli() + ':' + #interval",
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

        // 기본값 설정
        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();
        String validInterval = normalizeInterval(interval);

        // Validation & Adjustment
        validTo = validateAndAdjustDateRange(validFrom, validTo);

        log.debug("[Analytics] 매출 요약 집계 storeId={}", storeId);
        
        // 1. 현재 기간 데이터
        SalesSummaryResponse current = salesOrderSearchRepository.aggregateSalesSummary(storeId, validFrom, validTo);

        // 2. 이전 기간 계산
        OffsetDateTime[] previousPeriod = calculatePreviousPeriod(validFrom, validTo, validInterval);
        
        // 3. 이전 기간 데이터
        SalesSummaryResponse previous = salesOrderSearchRepository.aggregateSalesSummary(storeId, previousPeriod[0], previousPeriod[1]);

        // 4. 성장률 계산 및 반환
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


    private OffsetDateTime[] calculatePreviousPeriod(OffsetDateTime from, OffsetDateTime to, String interval) {
        long daysDiff = Duration.between(from, to).toDays() + 1;

        return switch (interval) {
            case "Month" -> new OffsetDateTime[]{from.minusMonths(1), to.minusMonths(1)};
            case "Week" -> new OffsetDateTime[]{from.minusWeeks(1), to.minusWeeks(1)};
            default -> new OffsetDateTime[]{from.minusDays(daysDiff), to.minusDays(daysDiff)};
        };
    }

    private Double calculateGrowthRate(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
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

    /**
     * interval 정규화 (소문자 → 대문자 첫글자)
     * day → Day, week → Week, month → Month
     */
    private String normalizeInterval(String interval) {
        if (interval == null) {
            return "Day";
        }

        return switch (interval.toLowerCase()) {
            case "day" -> "Day";
            case "week" -> "Week";
            case "month" -> "Month";
            default -> throw new AnalyticsException(AnalyticsErrorCode.INVALID_INTERVAL);
        };
    }

    /**
     * 날짜 범위 검증 및 조정
     * - from이 to보다 이후면 에러
     * - 미래 날짜(to)는 현재 시각으로 클램핑 (오늘 이후의 범위를 요청해도 오늘까지로 제한)
     * - 단, 시작일(from) 자체가 미래면 에러
     */
    private OffsetDateTime validateAndAdjustDateRange(OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime now = OffsetDateTime.now();

        // 1. 미래 날짜 방지: 시작일 자체가 미래면 에러
        if (from.isAfter(now)) {
            throw new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }

        // 2. 종료일이 미래면 현재 시각으로 조정 (클램핑)
        OffsetDateTime adjustedTo = to.isAfter(now) ? now : to;

        // 3. from > to 검증
        if (from.isAfter(adjustedTo)) {
            throw new AnalyticsException(AnalyticsErrorCode.INVALID_DATE_RANGE);
        }

        // 4. 최대 기간 검증 (1년)
        long daysBetween = Duration.between(from, adjustedTo).toDays();
        if (daysBetween > SalesAnalyticsConstants.MAX_QUERY_DAYS) {
            throw new AnalyticsException(AnalyticsErrorCode.DATE_RANGE_TOO_LONG);
        }

        return adjustedTo;
    }

    /**
     * topN 검증 (1 ~ 100)
     */
    private void validateTopN(int topN) {
        if (topN < SalesAnalyticsConstants.MIN_TOP_N || topN > SalesAnalyticsConstants.MAX_TOP_N) {
            throw new AnalyticsException(AnalyticsErrorCode.INVALID_TOP_N);
        }
    }
}