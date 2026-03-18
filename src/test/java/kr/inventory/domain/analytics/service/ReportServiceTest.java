package kr.inventory.domain.analytics.service;

import kr.inventory.domain.analytics.constant.SalesAnalyticsConstants;
import kr.inventory.domain.analytics.controller.dto.request.ReportSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.MenuRankingResponse;
import kr.inventory.domain.analytics.controller.dto.response.ReportSummaryResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesSummaryResponse;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.repository.ReportSearchRepositoryCustom;
import kr.inventory.domain.analytics.repository.SalesOrderSearchRepositoryCustom;
import kr.inventory.domain.analytics.service.report.RefundSection;
import kr.inventory.domain.analytics.service.report.StockInboundSection;
import kr.inventory.domain.analytics.service.report.WasteSection;
import kr.inventory.domain.store.service.StoreAccessValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("리포트 서비스 테스트")
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private StoreAccessValidator storeAccessValidator;

    @Mock
    private SalesOrderSearchRepositoryCustom salesOrderSearchRepository;

    @Mock
    private ReportSearchRepositoryCustom reportSearchRepository;

    @Mock
    private ReportPdfService reportPdfService;

    @Nested
    @DisplayName("사용자 지정 기간 리포트 발행")
    class GenerateReport {

        @Test
        @DisplayName("정상적인 기간으로 리포트를 발행하면 PDF 바이트 배열이 반환된다")
        void givenValidDateRange_whenGenerateReport_thenReturnPdfBytes() {
            // given
            Long userId = 100L;
            Long storeId = 1L;
            UUID storePublicId = UUID.randomUUID();
            LocalDate from = LocalDate.of(2025, 1, 1);
            LocalDate to = LocalDate.of(2025, 1, 31);
            ReportSearchRequest request = new ReportSearchRequest(from, to);

            byte[] expectedPdf = "test-pdf-content".getBytes();

            OffsetDateTime fromDt = from.atStartOfDay(ZoneId.of(SalesAnalyticsConstants.TIMEZONE_KST)).toOffsetDateTime();
            OffsetDateTime toDt = to.atTime(23, 59, 59).atZone(ZoneId.of(SalesAnalyticsConstants.TIMEZONE_KST)).toOffsetDateTime();

            SalesSummaryResponse salesSummary = new SalesSummaryResponse(
                    100L,
                    new BigDecimal("1000000.00"),
                    new BigDecimal("10000.00"),
                    new BigDecimal("50000.00"),
                    new BigDecimal("5000.00"),
                    0.0, 0.0, 0.0, 0.0
            );

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
            given(salesOrderSearchRepository.aggregateSalesSummary(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(salesSummary);
            given(salesOrderSearchRepository.aggregateMenuRanking(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), anyInt(), eq("amount")))
                    .willReturn(Collections.emptyList());
            given(reportSearchRepository.aggregateRefundSection(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), anyLong()))
                    .willReturn(new RefundSection(5L, new BigDecimal("50000.00"), 5.0));
            given(reportSearchRepository.aggregateWasteSection(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(new WasteSection(new BigDecimal("30000.00"), new BigDecimal("50.000"), Collections.emptyList(), Collections.emptyList()));
            given(reportSearchRepository.aggregateStockInboundSection(eq(storeId), eq(from), eq(to)))
                    .willReturn(new StockInboundSection(10L, Collections.emptyList()));
            given(reportPdfService.generate(any(), eq(from), eq(to))).willReturn(expectedPdf);

            // when
            byte[] result = reportService.generateReport(userId, storePublicId, request);

            // then
            assertThat(result).isEqualTo(expectedPdf);
            verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
            verify(reportPdfService).generate(any(), eq(from), eq(to));
        }

        @Test
        @DisplayName("from이 미래 날짜면 예외가 발생한다")
        void givenFutureFromDate_whenGenerateReport_thenThrowException() {
            // given
            Long userId = 100L;
            UUID storePublicId = UUID.randomUUID();
            LocalDate from = LocalDate.now().plusDays(1);
            LocalDate to = LocalDate.now().plusDays(10);
            ReportSearchRequest request = new ReportSearchRequest(from, to);

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> reportService.generateReport(userId, storePublicId, request))
                    .isInstanceOf(AnalyticsException.class)
                    .extracting("errorModel")
                    .isEqualTo(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("to가 미래 날짜면 예외가 발생한다")
        void givenFutureToDate_whenGenerateReport_thenThrowException() {
            // given
            Long userId = 100L;
            UUID storePublicId = UUID.randomUUID();
            LocalDate from = LocalDate.now().minusDays(10);
            LocalDate to = LocalDate.now().plusDays(1);
            ReportSearchRequest request = new ReportSearchRequest(from, to);

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> reportService.generateReport(userId, storePublicId, request))
                    .isInstanceOf(AnalyticsException.class)
                    .extracting("errorModel")
                    .isEqualTo(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("from이 to보다 나중이면 예외가 발생한다")
        void givenFromAfterTo_whenGenerateReport_thenThrowException() {
            // given
            Long userId = 100L;
            UUID storePublicId = UUID.randomUUID();
            LocalDate from = LocalDate.of(2025, 2, 1);
            LocalDate to = LocalDate.of(2025, 1, 1);
            ReportSearchRequest request = new ReportSearchRequest(from, to);

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> reportService.generateReport(userId, storePublicId, request))
                    .isInstanceOf(AnalyticsException.class)
                    .extracting("errorModel")
                    .isEqualTo(AnalyticsErrorCode.INVALID_DATE_RANGE);
        }

        @Test
        @DisplayName("기간이 MAX_QUERY_DAYS를 초과하면 예외가 발생한다")
        void givenDateRangeTooLong_whenGenerateReport_thenThrowException() {
            // given
            Long userId = 100L;
            UUID storePublicId = UUID.randomUUID();
            LocalDate from = LocalDate.now().minusDays(SalesAnalyticsConstants.MAX_QUERY_DAYS + 2);
            LocalDate to = LocalDate.now();
            ReportSearchRequest request = new ReportSearchRequest(from, to);

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> reportService.generateReport(userId, storePublicId, request))
                    .isInstanceOf(AnalyticsException.class)
                    .extracting("errorModel")
                    .isEqualTo(AnalyticsErrorCode.DATE_RANGE_TOO_LONG);
        }

        @Test
        @DisplayName("메뉴 랭킹 데이터가 포함된 리포트를 생성한다")
        void givenMenuRankingData_whenGenerateReport_thenIncludeMenuRanking() {
            // given
            Long userId = 100L;
            Long storeId = 1L;
            UUID storePublicId = UUID.randomUUID();
            LocalDate from = LocalDate.of(2025, 1, 1);
            LocalDate to = LocalDate.of(2025, 1, 31);
            ReportSearchRequest request = new ReportSearchRequest(from, to);

            byte[] expectedPdf = "test-pdf-content".getBytes();

            SalesSummaryResponse salesSummary = new SalesSummaryResponse(
                    100L,
                    new BigDecimal("1000000.00"),
                    new BigDecimal("10000.00"),
                    new BigDecimal("50000.00"),
                    new BigDecimal("5000.00"),
                    0.0, 0.0, 0.0, 0.0
            );

            List<MenuRankingResponse> menuRanking = List.of(
                    new MenuRankingResponse(1, "김치찌개", 50L, new BigDecimal("250000.00")),
                    new MenuRankingResponse(2, "된장찌개", 40L, new BigDecimal("200000.00"))
            );

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
            given(salesOrderSearchRepository.aggregateSalesSummary(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(salesSummary);
            given(salesOrderSearchRepository.aggregateMenuRanking(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), anyInt(), eq("amount")))
                    .willReturn(menuRanking);
            given(reportSearchRepository.aggregateRefundSection(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), anyLong()))
                    .willReturn(new RefundSection(5L, new BigDecimal("50000.00"), 5.0));
            given(reportSearchRepository.aggregateWasteSection(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(new WasteSection(new BigDecimal("30000.00"), new BigDecimal("50.000"), Collections.emptyList(), Collections.emptyList()));
            given(reportSearchRepository.aggregateStockInboundSection(eq(storeId), eq(from), eq(to)))
                    .willReturn(new StockInboundSection(10L, Collections.emptyList()));
            given(reportPdfService.generate(any(), eq(from), eq(to))).willReturn(expectedPdf);

            // when
            byte[] result = reportService.generateReport(userId, storePublicId, request);

            // then
            assertThat(result).isEqualTo(expectedPdf);
            verify(salesOrderSearchRepository).aggregateMenuRanking(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), eq(5), eq("amount"));
        }
    }

    @Nested
    @DisplayName("월간 리포트 조회")
    class GenerateMonthlyReport {

        @Test
        @DisplayName("전월 yearMonth로 월간 리포트를 발행하면 PDF 바이트 배열이 반환된다")
        void givenValidYearMonth_whenGenerateMonthlyReport_thenReturnPdfBytes() {
            // given
            Long userId = 100L;
            Long storeId = 1L;
            UUID storePublicId = UUID.randomUUID();
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            String yearMonth = lastMonth.toString();

            byte[] expectedPdf = "test-pdf-content".getBytes();

            LocalDate from = lastMonth.atDay(1);
            LocalDate to = lastMonth.atEndOfMonth();

            SalesSummaryResponse salesSummary = new SalesSummaryResponse(
                    200L,
                    new BigDecimal("2000000.00"),
                    new BigDecimal("10000.00"),
                    new BigDecimal("50000.00"),
                    new BigDecimal("5000.00"),
                    0.0, 0.0, 0.0, 0.0
            );

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
            given(salesOrderSearchRepository.aggregateSalesSummary(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(salesSummary);
            given(salesOrderSearchRepository.aggregateMenuRanking(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), anyInt(), eq("amount")))
                    .willReturn(Collections.emptyList());
            given(reportSearchRepository.aggregateRefundSection(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), anyLong()))
                    .willReturn(new RefundSection(10L, new BigDecimal("100000.00"), 5.0));
            given(reportSearchRepository.aggregateWasteSection(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(new WasteSection(new BigDecimal("60000.00"), new BigDecimal("100.000"), Collections.emptyList(), Collections.emptyList()));
            given(reportSearchRepository.aggregateStockInboundSection(eq(storeId), eq(from), eq(to)))
                    .willReturn(new StockInboundSection(20L, Collections.emptyList()));
            given(reportPdfService.generate(any(), eq(from), eq(to))).willReturn(expectedPdf);

            // when
            byte[] result = reportService.generateMonthlyReport(userId, storePublicId, yearMonth);

            // then
            assertThat(result).isEqualTo(expectedPdf);
            verify(storeAccessValidator).validateAndGetStoreId(userId, storePublicId);
            verify(reportPdfService).generate(any(), eq(from), eq(to));
        }

        @Test
        @DisplayName("당월을 조회하면 예외가 발생한다")
        void givenCurrentMonth_whenGenerateMonthlyReport_thenThrowException() {
            // given
            Long userId = 100L;
            UUID storePublicId = UUID.randomUUID();
            String yearMonth = YearMonth.now().toString();

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> reportService.generateMonthlyReport(userId, storePublicId, yearMonth))
                    .isInstanceOf(AnalyticsException.class)
                    .extracting("errorModel")
                    .isEqualTo(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("미래 월을 조회하면 예외가 발생한다")
        void givenFutureMonth_whenGenerateMonthlyReport_thenThrowException() {
            // given
            Long userId = 100L;
            UUID storePublicId = UUID.randomUUID();
            String yearMonth = YearMonth.now().plusMonths(1).toString();

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> reportService.generateMonthlyReport(userId, storePublicId, yearMonth))
                    .isInstanceOf(AnalyticsException.class)
                    .extracting("errorModel")
                    .isEqualTo(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }

        @Test
        @DisplayName("잘못된 yearMonth 형식이면 예외가 발생한다")
        void givenInvalidYearMonthFormat_whenGenerateMonthlyReport_thenThrowException() {
            // given
            Long userId = 100L;
            UUID storePublicId = UUID.randomUUID();
            String yearMonth = "2025-13"; // 잘못된 월

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> reportService.generateMonthlyReport(userId, storePublicId, yearMonth))
                    .isInstanceOf(AnalyticsException.class)
                    .extracting("errorModel")
                    .isEqualTo(AnalyticsErrorCode.INVALID_DATE_RANGE);
        }

        @Test
        @DisplayName("yearMonth가 yyyy-MM 형식이 아니면 예외가 발생한다")
        void givenWrongYearMonthFormat_whenGenerateMonthlyReport_thenThrowException() {
            // given
            Long userId = 100L;
            UUID storePublicId = UUID.randomUUID();
            String yearMonth = "202501"; // yyyy-MM 형식이 아님

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> reportService.generateMonthlyReport(userId, storePublicId, yearMonth))
                    .isInstanceOf(AnalyticsException.class)
                    .extracting("errorModel")
                    .isEqualTo(AnalyticsErrorCode.INVALID_DATE_RANGE);
        }
    }

    @Nested
    @DisplayName("리포트 요약 조회 (JSON)")
    class GetReportSummary {

        @Test
        @DisplayName("정상적인 기간으로 요약을 조회하면 ReportSummaryResponse가 반환된다")
        void givenValidDateRange_whenGetReportSummary_thenReturnSummaryResponse() {
            // given
            Long userId = 100L;
            Long storeId = 1L;
            UUID storePublicId = UUID.randomUUID();
            LocalDate from = LocalDate.of(2025, 1, 1);
            LocalDate to = LocalDate.of(2025, 1, 31);
            ReportSearchRequest request = new ReportSearchRequest(from, to);

            SalesSummaryResponse salesSummary = new SalesSummaryResponse(
                    100L,
                    new BigDecimal("1000000.00"),
                    new BigDecimal("10000.00"),
                    new BigDecimal("50000.00"),
                    new BigDecimal("5000.00"),
                    0.0, 0.0, 0.0, 0.0
            );

            List<MenuRankingResponse> menuRanking = List.of(
                    new MenuRankingResponse(1, "김치찌개", 50L, new BigDecimal("250000.00")),
                    new MenuRankingResponse(2, "된장찌개", 40L, new BigDecimal("200000.00"))
            );

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
            given(salesOrderSearchRepository.aggregateSalesSummary(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(salesSummary);
            given(salesOrderSearchRepository.aggregateMenuRanking(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), anyInt(), eq("amount")))
                    .willReturn(menuRanking);
            given(reportSearchRepository.aggregateRefundSection(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), anyLong()))
                    .willReturn(new RefundSection(5L, new BigDecimal("50000.00"), 5.0));
            given(reportSearchRepository.aggregateWasteSection(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(new WasteSection(new BigDecimal("30000.00"), new BigDecimal("50.000"),
                        Collections.emptyList(), Collections.emptyList()));
            given(reportSearchRepository.aggregateStockInboundSection(eq(storeId), eq(from), eq(to)))
                    .willReturn(new StockInboundSection(10L, Collections.emptyList()));

            // when
            ReportSummaryResponse result = reportService.getReportSummary(userId, storePublicId, request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.from()).isEqualTo(from);
            assertThat(result.to()).isEqualTo(to);
            assertThat(result.totalOrderCount()).isEqualTo(100L);
            assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
            assertThat(result.averageOrderAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
            assertThat(result.menuTop5()).hasSize(2);
            assertThat(result.menuTop5().get(0).menuName()).isEqualTo("김치찌개");
            assertThat(result.refundCount()).isEqualTo(5L);
            assertThat(result.refundRate()).isEqualTo(5.0);
            assertThat(result.totalWasteAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));
            assertThat(result.totalInboundCount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("from이 to보다 나중이면 예외가 발생한다")
        void givenFromAfterTo_whenGetReportSummary_thenThrowException() {
            // given
            Long userId = 100L;
            UUID storePublicId = UUID.randomUUID();
            LocalDate from = LocalDate.of(2025, 2, 1);
            LocalDate to = LocalDate.of(2025, 1, 1);
            ReportSearchRequest request = new ReportSearchRequest(from, to);

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> reportService.getReportSummary(userId, storePublicId, request))
                    .isInstanceOf(AnalyticsException.class)
                    .extracting("errorModel")
                    .isEqualTo(AnalyticsErrorCode.INVALID_DATE_RANGE);
        }
    }

    @Nested
    @DisplayName("월간 리포트 요약 조회 (JSON)")
    class GetMonthlyReportSummary {

        @Test
        @DisplayName("전월 yearMonth로 요약을 조회하면 ReportSummaryResponse가 반환된다")
        void givenValidYearMonth_whenGetMonthlyReportSummary_thenReturnSummaryResponse() {
            // given
            Long userId = 100L;
            Long storeId = 1L;
            UUID storePublicId = UUID.randomUUID();
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            String yearMonth = lastMonth.toString();

            LocalDate from = lastMonth.atDay(1);
            LocalDate to = lastMonth.atEndOfMonth();

            SalesSummaryResponse salesSummary = new SalesSummaryResponse(
                    200L,
                    new BigDecimal("2000000.00"),
                    new BigDecimal("10000.00"),
                    new BigDecimal("50000.00"),
                    new BigDecimal("5000.00"),
                    0.0, 0.0, 0.0, 0.0
            );

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(storeId);
            given(salesOrderSearchRepository.aggregateSalesSummary(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(salesSummary);
            given(salesOrderSearchRepository.aggregateMenuRanking(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), anyInt(), eq("amount")))
                    .willReturn(Collections.emptyList());
            given(reportSearchRepository.aggregateRefundSection(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class), anyLong()))
                    .willReturn(new RefundSection(10L, new BigDecimal("100000.00"), 5.0));
            given(reportSearchRepository.aggregateWasteSection(eq(storeId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                    .willReturn(new WasteSection(new BigDecimal("60000.00"), new BigDecimal("100.000"),
                        Collections.emptyList(), Collections.emptyList()));
            given(reportSearchRepository.aggregateStockInboundSection(eq(storeId), eq(from), eq(to)))
                    .willReturn(new StockInboundSection(20L, Collections.emptyList()));

            // when
            ReportSummaryResponse result = reportService.getMonthlyReportSummary(userId, storePublicId, yearMonth);

            // then
            assertThat(result).isNotNull();
            assertThat(result.from()).isEqualTo(from);
            assertThat(result.to()).isEqualTo(to);
            assertThat(result.totalOrderCount()).isEqualTo(200L);
            assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("2000000.00"));
            assertThat(result.refundCount()).isEqualTo(10L);
            assertThat(result.totalWasteAmount()).isEqualByComparingTo(new BigDecimal("60000.00"));
            assertThat(result.totalInboundCount()).isEqualTo(20L);
        }

        @Test
        @DisplayName("당월을 조회하면 예외가 발생한다")
        void givenCurrentMonth_whenGetMonthlyReportSummary_thenThrowException() {
            // given
            Long userId = 100L;
            UUID storePublicId = UUID.randomUUID();
            String yearMonth = YearMonth.now().toString();

            given(storeAccessValidator.validateAndGetStoreId(userId, storePublicId)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> reportService.getMonthlyReportSummary(userId, storePublicId, yearMonth))
                    .isInstanceOf(AnalyticsException.class)
                    .extracting("errorModel")
                    .isEqualTo(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }
    }
}