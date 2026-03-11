package kr.inventory.domain.analytics.service;

import kr.inventory.domain.analytics.constant.SalesAnalyticsConstants;
import kr.inventory.domain.analytics.controller.dto.response.MenuRankingResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesPeakResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesSummaryResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesTrendResponse;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.repository.SalesOrderSearchRepositoryCustom;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
     * TTL: RedisConfig 기본값 30분
     */
    @Cacheable(
            value = "sales:trend",
            key = "#storePublicId + ':' + #from.toInstant().toEpochMilli() + ':' + #to.toInstant().toEpochMilli() + ':' + #interval"
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

        // Validation
        validateDateRange(validFrom, validTo);

        log.debug("[Analytics] 매출 추이 집계 storeId={} interval={}", storeId, validInterval);
        return salesOrderSearchRepository.aggregateSalesTrend(storeId, validFrom, validTo, validInterval);
    }

    /**
     * 요일×시간대 피크
     * TTL: 1시간 (피크 패턴은 자주 안 바뀜)
     */
    @Cacheable(
            value = "sales:peak",
            key = "#storePublicId + ':' + #from.toInstant().toEpochMilli() + ':' + #to.toInstant().toEpochMilli()"
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

        // Validation
        validateDateRange(validFrom, validTo);

        log.debug("[Analytics] 피크 집계 storeId={}", storeId);
        return salesOrderSearchRepository.aggregateSalesPeak(storeId, validFrom, validTo);
    }

    /**
     * 메뉴 TOP N
     * TTL: 30분
     */
    @Cacheable(
            value = "sales:menu-ranking",
            key = "#storePublicId + ':' + #from.toInstant().toEpochMilli() + ':' + #to.toInstant().toEpochMilli() + ':' + #topN"
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

        // Validation
        validateDateRange(validFrom, validTo);
        validateTopN(validTopN);

        log.debug("[Analytics] 메뉴 랭킹 집계 storeId={} topN={}", storeId, validTopN);
        return salesOrderSearchRepository.aggregateMenuRanking(storeId, validFrom, validTo, validTopN);
    }

    /**
     * 매출 요약 (객단가 등)
     * TTL: 30분
     */
    @Cacheable(
            value = "sales:summary",
            key = "#storePublicId + ':' + #from.toInstant().toEpochMilli() + ':' + #to.toInstant().toEpochMilli()"
    )
    public SalesSummaryResponse getSalesSummary(
            Long userId,
            UUID storePublicId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        // 기본값 설정
        OffsetDateTime validFrom = (from != null) ? from : OffsetDateTime.now().minusDays(SalesAnalyticsConstants.DEFAULT_DAYS_BACK);
        OffsetDateTime validTo = (to != null) ? to : OffsetDateTime.now();

        // Validation
        validateDateRange(validFrom, validTo);

        log.debug("[Analytics] 매출 요약 집계 storeId={}", storeId);
        return salesOrderSearchRepository.aggregateSalesSummary(storeId, validFrom, validTo);
    }

    // ────────────────────────────────────────────────────────
    // Private Helper Methods
    // ────────────────────────────────────────────────────────

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
     * 날짜 범위 검증
     * - from이 to보다 이후면 에러
     * - 최대 조회 기간 초과 시 에러
     * - 미래 날짜 조회 방지
     */
    private void validateDateRange(OffsetDateTime from, OffsetDateTime to) {
        // from > to 검증
        if (from.isAfter(to)) {
            throw new AnalyticsException(AnalyticsErrorCode.INVALID_DATE_RANGE);
        }

        // 최대 기간 검증 (1년)
        long daysBetween = Duration.between(from, to).toDays();
        if (daysBetween > SalesAnalyticsConstants.MAX_QUERY_DAYS) {
            throw new AnalyticsException(AnalyticsErrorCode.DATE_RANGE_TOO_LONG);
        }

        // 미래 날짜 방지
        OffsetDateTime now = OffsetDateTime.now();
        if (to.isAfter(now)) {
            throw new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }
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