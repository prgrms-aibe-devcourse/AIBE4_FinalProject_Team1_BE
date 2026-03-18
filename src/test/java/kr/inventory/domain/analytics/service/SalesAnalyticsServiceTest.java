package kr.inventory.domain.analytics.service;

import kr.inventory.domain.analytics.controller.dto.response.*;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.repository.SalesOrderSearchRepositoryCustom;
import kr.inventory.domain.store.service.StoreAccessValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("매출 분석 서비스 테스트")
class SalesAnalyticsServiceTest {

    @InjectMocks
    private SalesAnalyticsService salesAnalyticsService;

    @Mock
    private StoreAccessValidator storeAccessValidator;

    @Mock
    private SalesOrderSearchRepositoryCustom salesOrderSearchRepository;

    @ParameterizedTest
    @CsvSource({
            "day,Day",
            "week,Week",
            "month,Month"
    })
    @DisplayName("매출 추이 조회 시 interval(day/week/month)을 정규화하여 ES 집계를 호출한다")
    void givenInterval_whenGetSalesTrend_thenNormalizeAndCallRepository(String requestInterval, String expectedInterval) {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(31);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

        List<SalesTrendResponse> expected = List.of(
                new SalesTrendResponse("2025-01-01", 5L, new BigDecimal("120000.00"))
        );

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(salesOrderSearchRepository.aggregateSalesTrend(storeId, from, to, expectedInterval)).willReturn(expected);

        List<SalesTrendResponse> actual = salesAnalyticsService.getSalesTrend(userId, storePublicId, from, to, requestInterval);

        assertThat(actual).isEqualTo(expected);
        verify(salesOrderSearchRepository).aggregateSalesTrend(storeId, from, to, expectedInterval);
    }

    @Test
    @DisplayName("매출 추이 조회 시 interval이 null이면 Day로 처리한다")
    void givenNullInterval_whenGetSalesTrend_thenUseDay() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(salesOrderSearchRepository.aggregateSalesTrend(storeId, from, to, "Day")).willReturn(List.of());

        salesAnalyticsService.getSalesTrend(userId, storePublicId, from, to, null);

        verify(salesOrderSearchRepository).aggregateSalesTrend(storeId, from, to, "Day");
    }

    @Test
    @DisplayName("매출 추이 조회 시 interval이 유효하지 않으면 예외가 발생한다")
    void givenInvalidInterval_whenGetSalesTrend_thenThrowException() {
        Long userId = 100L;
        UUID storePublicId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(10);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

        assertThatThrownBy(() -> salesAnalyticsService.getSalesTrend(userId, storePublicId, from, to, "year"))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.INVALID_INTERVAL);
    }

    @Test
    @DisplayName("매출 요약 조회 시 week 기준으로 이전 기간을 계산해 성장률을 계산한다")
    void givenWeekInterval_whenGetSalesSummary_thenCalculateGrowthFromPreviousWeek() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(14);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(8);

        SalesSummaryResponse current = new SalesSummaryResponse(
                20L,
                new BigDecimal("200000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("5000.00"),
                null, null, null, null
        );

        SalesSummaryResponse previous = new SalesSummaryResponse(
                10L,
                new BigDecimal("100000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("3000.00"),
                null, null, null, null
        );

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(salesOrderSearchRepository.aggregateSalesSummary(storeId, from, to)).willReturn(current);
        given(salesOrderSearchRepository.aggregateSalesSummary(storeId, from.minusWeeks(1), to.minusWeeks(1)))
                .willReturn(previous);

        SalesSummaryResponse result = salesAnalyticsService.getSalesSummary(userId, storePublicId, from, to, "week");

        assertThat(result.orderCountGrowthRate()).isEqualTo(100.0);
        assertThat(result.totalAmountGrowthRate()).isEqualTo(100.0);
        assertThat(result.avgAmountGrowthRate()).isEqualTo(0.0);
        assertThat(result.maxAmountGrowthRate()).isEqualTo(50.0);

        verify(salesOrderSearchRepository).aggregateSalesSummary(storeId, from, to);
        verify(salesOrderSearchRepository).aggregateSalesSummary(storeId, from.minusWeeks(1), to.minusWeeks(1));
    }

    @Test
    @DisplayName("매출 요약 조회 시 month 기준으로 이전 기간을 계산한다")
    void givenMonthInterval_whenGetSalesSummary_thenUsePreviousMonthRange() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(60);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(32);

        SalesSummaryResponse current = new SalesSummaryResponse(1L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                null, null, null, null);
        SalesSummaryResponse previous = new SalesSummaryResponse(0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, null, null, null);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(salesOrderSearchRepository.aggregateSalesSummary(storeId, from, to)).willReturn(current);
        given(salesOrderSearchRepository.aggregateSalesSummary(storeId, from.minusMonths(1), to.minusMonths(1)))
                .willReturn(previous);

        salesAnalyticsService.getSalesSummary(userId, storePublicId, from, to, "month");

        verify(salesOrderSearchRepository).aggregateSalesSummary(storeId, from.minusMonths(1), to.minusMonths(1));
    }

    @Test
    @DisplayName("매출 피크 조회 시 repository 결과를 그대로 반환한다")
    void givenValidRange_whenGetSalesPeak_thenReturnRepositoryResult() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

        List<SalesPeakResponse> expected = List.of(new SalesPeakResponse(1, 12, 7L));

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(salesOrderSearchRepository.aggregateSalesPeak(storeId, from, to)).willReturn(expected);

        List<SalesPeakResponse> actual = salesAnalyticsService.getSalesPeak(userId, storePublicId, from, to);

        assertThat(actual).isEqualTo(expected);
        verify(salesOrderSearchRepository).aggregateSalesPeak(storeId, from, to);
    }

    @Test
    @DisplayName("메뉴 랭킹 조회 시 topN 기본값(10)을 사용한다")
    void givenNullTopN_whenGetMenuRanking_thenUseDefaultTopN() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(10);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(salesOrderSearchRepository.aggregateMenuRanking(eq(storeId), eq(from), eq(to), eq(10), eq("quantity"))).willReturn(List.of());

        salesAnalyticsService.getMenuRanking(userId, storePublicId, from, to, null);

        verify(salesOrderSearchRepository).aggregateMenuRanking(storeId, from, to, 10, "quantity");
    }

    @Test
    @DisplayName("메뉴 랭킹 조회 시 topN이 범위를 벗어나면 예외가 발생한다")
    void givenInvalidTopN_whenGetMenuRanking_thenThrowException() {
        Long userId = 100L;
        UUID storePublicId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(10);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

        assertThatThrownBy(() -> salesAnalyticsService.getMenuRanking(userId, storePublicId, from, to, 101))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.INVALID_TOP_N);
    }

    @Test
    @DisplayName("매출 추이 조회 시 from > to 이면 INVALID_DATE_RANGE 예외가 발생한다")
    void givenFromAfterTo_whenGetSalesTrend_thenThrowInvalidDateRange() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(3);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        assertThatThrownBy(() -> salesAnalyticsService.getSalesTrend(userId, storePublicId, from, to, "day"))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("매출 추이 조회 시 from이 미래면 FUTURE_DATE_NOT_ALLOWED 예외가 발생한다")
    void givenFutureFrom_whenGetSalesTrend_thenThrowFutureDateNotAllowed() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        assertThatThrownBy(() -> salesAnalyticsService.getSalesTrend(userId, storePublicId, from, to, "day"))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("매출 추이 조회 시 조회 기간이 1년 초과면 DATE_RANGE_TOO_LONG 예외가 발생한다")
    void givenTooLongRange_whenGetSalesTrend_thenThrowDateRangeTooLong() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(366);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        assertThatThrownBy(() -> salesAnalyticsService.getSalesTrend(userId, storePublicId, from, to, "day"))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.DATE_RANGE_TOO_LONG);
    }

    @Test
    @DisplayName("환불 요약 조회 시 repository 결과를 그대로 반환한다")
    void givenValidRange_whenGetRefundSummary_thenReturnRepositoryResult() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

        RefundSummaryResponse expected = new RefundSummaryResponse(
                5L,
                new BigDecimal("25000.00"),
                4.8
        );

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(salesOrderSearchRepository.aggregateRefundSummary(storeId, from, to)).willReturn(expected);

        RefundSummaryResponse actual = salesAnalyticsService.getRefundSummary(userId, storePublicId, from, to);

        assertThat(actual).isEqualTo(expected);
        verify(salesOrderSearchRepository).aggregateRefundSummary(storeId, from, to);
    }

    @Test
    @DisplayName("환불 요약 조회 시 from이 미래면 FUTURE_DATE_NOT_ALLOWED 예외가 발생한다")
    void givenFutureFrom_whenGetRefundSummary_thenThrowFutureDateNotAllowed() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        assertThatThrownBy(() -> salesAnalyticsService.getRefundSummary(userId, storePublicId, from, to))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("환불 요약 조회 시 from > to 이면 INVALID_DATE_RANGE 예외가 발생한다")
    void givenFromAfterTo_whenGetRefundSummary_thenThrowInvalidDateRange() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(3);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        assertThatThrownBy(() -> salesAnalyticsService.getRefundSummary(userId, storePublicId, from, to))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("환불 요약 조회 시 조회 기간이 1년 초과면 DATE_RANGE_TOO_LONG 예외가 발생한다")
    void givenTooLongRange_whenGetRefundSummary_thenThrowDateRangeTooLong() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(366);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        assertThatThrownBy(() -> salesAnalyticsService.getRefundSummary(userId, storePublicId, from, to))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.DATE_RANGE_TOO_LONG);
    }

    @Test
    @DisplayName("환불 요약 조회 시 from/to가 null이면 기본값(최근 7일)으로 조회한다")
    void givenNullDates_whenGetRefundSummary_thenUseDefaultRange() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        RefundSummaryResponse expected = new RefundSummaryResponse(0L, BigDecimal.ZERO, 0.0);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(salesOrderSearchRepository.aggregateRefundSummary(
                eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .willReturn(expected);

        RefundSummaryResponse actual = salesAnalyticsService.getRefundSummary(userId, storePublicId, null, null);

        assertThat(actual).isEqualTo(expected);
        verify(salesOrderSearchRepository).aggregateRefundSummary(
                eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("메뉴 상세 조회 시 repository 결과를 그대로 반환한다")
    void givenValidMenuName_whenGetMenuSalesDetail_thenReturnRepositoryResult() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();
        String menuName = "아메리카노";

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

        MenuSalesDetailResponse expected = new MenuSalesDetailResponse(
                menuName,
                120L,
                new BigDecimal("480000.00"),
                new BigDecimal("4000.00"),
                25.0
        );

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(salesOrderSearchRepository.aggregateMenuSalesDetail(storeId, from, to, menuName)).willReturn(expected);

        MenuSalesDetailResponse actual = salesAnalyticsService.getMenuSalesDetail(
                userId, storePublicId, from, to, menuName);

        assertThat(actual).isEqualTo(expected);
        verify(salesOrderSearchRepository).aggregateMenuSalesDetail(storeId, from, to, menuName);
    }

    @Test
    @DisplayName("메뉴 상세 조회 시 menuName이 null이면 INVALID_MENU_NAME 예외가 발생한다")
    void givenNullMenuName_whenGetMenuSalesDetail_thenThrowInvalidMenuName() {
        Long userId = 100L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

        assertThatThrownBy(() -> salesAnalyticsService.getMenuSalesDetail(
                userId, storePublicId, from, to, null))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.INVALID_MENU_NAME);
    }

    @Test
    @DisplayName("메뉴 상세 조회 시 menuName이 빈 문자열이면 INVALID_MENU_NAME 예외가 발생한다")
    void givenBlankMenuName_whenGetMenuSalesDetail_thenThrowInvalidMenuName() {
        Long userId = 100L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);

        assertThatThrownBy(() -> salesAnalyticsService.getMenuSalesDetail(
                userId, storePublicId, from, to, "   "))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.INVALID_MENU_NAME);
    }

    @Test
    @DisplayName("메뉴 상세 조회 시 from이 미래면 FUTURE_DATE_NOT_ALLOWED 예외가 발생한다")
    void givenFutureFrom_whenGetMenuSalesDetail_thenThrowFutureDateNotAllowed() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).plusDays(2);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        assertThatThrownBy(() -> salesAnalyticsService.getMenuSalesDetail(
                userId, storePublicId, from, to, "아메리카노"))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("메뉴 상세 조회 시 from > to 이면 INVALID_DATE_RANGE 예외가 발생한다")
    void givenFromAfterTo_whenGetMenuSalesDetail_thenThrowInvalidDateRange() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();

        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime to = OffsetDateTime.now(ZoneOffset.UTC).minusDays(3);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);

        assertThatThrownBy(() -> salesAnalyticsService.getMenuSalesDetail(
                userId, storePublicId, from, to, "아메리카노"))
                .isInstanceOf(AnalyticsException.class)
                .extracting("errorModel")
                .isEqualTo(AnalyticsErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("메뉴 상세 조회 시 from/to가 null이면 기본값(최근 7일)으로 조회한다")
    void givenNullDates_whenGetMenuSalesDetail_thenUseDefaultRange() {
        Long userId = 100L;
        Long storeId = 1L;
        UUID storePublicId = UUID.randomUUID();
        String menuName = "아메리카노";

        MenuSalesDetailResponse expected = new MenuSalesDetailResponse(
                menuName, 0L, BigDecimal.ZERO, BigDecimal.ZERO, 0.0);

        given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
        given(salesOrderSearchRepository.aggregateMenuSalesDetail(
                eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), eq(menuName)))
                .willReturn(expected);

        MenuSalesDetailResponse actual = salesAnalyticsService.getMenuSalesDetail(
                userId, storePublicId, null, null, menuName);

        assertThat(actual).isEqualTo(expected);
    }
}
