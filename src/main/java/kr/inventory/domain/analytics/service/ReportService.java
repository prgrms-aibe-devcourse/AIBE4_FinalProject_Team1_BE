package kr.inventory.domain.analytics.service;

import kr.inventory.domain.analytics.constant.ReportConstants;
import kr.inventory.domain.analytics.constant.SalesAnalyticsConstants;
import kr.inventory.domain.analytics.controller.dto.request.ReportSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.MenuRankingResponse;
import kr.inventory.domain.analytics.controller.dto.response.ReportSummaryResponse;
import kr.inventory.domain.analytics.controller.dto.response.SalesSummaryResponse;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.repository.ReportSearchRepositoryCustom;
import kr.inventory.domain.analytics.repository.SalesOrderSearchRepositoryCustom;
import kr.inventory.domain.analytics.service.report.*;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final StoreAccessValidator storeAccessValidator;
    private final SalesOrderSearchRepositoryCustom salesOrderSearchRepository;
    private final ReportSearchRepositoryCustom reportSearchRepository;
    private final ReportPdfService reportPdfService;

    /**
     * 리포트 요약 조회 (JSON)
     */
    public ReportSummaryResponse getReportSummary(Long userId, UUID storePublicId, ReportSearchRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        LocalDate from = request.from();
        LocalDate to = request.to();
        validateDateRange(from, to);

        ReportData reportData = buildReportData(storeId, from, to);
        return ReportSummaryResponse.from(reportData, from, to);
    }

    /**
     * 월간 리포트 요약 조회 (JSON)
     */
    public ReportSummaryResponse getMonthlyReportSummary(Long userId, UUID storePublicId, String yearMonth) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        YearMonth ym = parseYearMonth(yearMonth);
        if (!ym.isBefore(YearMonth.now())) {
            throw new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }

        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        ReportData reportData = buildReportData(storeId, from, to);
        return ReportSummaryResponse.from(reportData, from, to);
    }

    /**
     * 사용자 지정 기간 리포트 발행
     */
    public byte[] generateReport(Long userId, UUID storePublicId, ReportSearchRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        LocalDate from = request.from();
        LocalDate to = request.to();
        validateDateRange(from, to);

        ReportData reportData = buildReportData(storeId, from, to);
        log.info("[Report] 리포트 생성 완료 storeId={} from={} to={}", storeId, from, to);

        return reportPdfService.generate(reportData, from, to);
    }

    /**
     * 월간 리포트 (전월 1일 ~ 말일 고정)
     */
    public byte[] generateMonthlyReport(Long userId, UUID storePublicId, String yearMonth) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        YearMonth ym = parseYearMonth(yearMonth);

        // 당월 또는 미래 월 방지 — 전월까지만 허용
        if (!ym.isBefore(YearMonth.now())) {
            throw new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }

        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        ReportData reportData = buildReportData(storeId, from, to);
        log.info("[Report] 월간 리포트 생성 완료 storeId={} yearMonth={}", storeId, yearMonth);

        return reportPdfService.generate(reportData, from, to);
    }

    // ──────────────────────────────────────────────────────
    // Private Methods
    // ──────────────────────────────────────────────────────

    private ReportData buildReportData(Long storeId, LocalDate from, LocalDate to) {
        // OffsetDateTime 변환 (KST 기준, ES 쿼리용)
        OffsetDateTime fromDt = from.atStartOfDay(ZoneId.of(SalesAnalyticsConstants.TIMEZONE_KST))
                .toOffsetDateTime();
        OffsetDateTime toDt = to.atTime(23, 59, 59)
                .atZone(ZoneId.of(SalesAnalyticsConstants.TIMEZONE_KST))
                .toOffsetDateTime();

        // 1. 매출 섹션
        SalesSummaryResponse summary = salesOrderSearchRepository.aggregateSalesSummary(
                storeId, fromDt, toDt);
        List<MenuRankingResponse> menuRanking = salesOrderSearchRepository.aggregateMenuRanking(
                storeId, fromDt, toDt, ReportConstants.REPORT_TOP_N_MENU);

        SalesSection salesSection = new SalesSection(
                summary.totalOrderCount(),
                summary.totalAmount(),
                summary.averageOrderAmount(),
                summary.maxOrderAmount(),
                menuRanking.stream()
                        .map(m -> new SalesSection.MenuEntry(
                                m.rank(), m.menuName(), m.totalQuantity(), m.totalAmount()))
                        .toList()
        );

        // 2. 환불 섹션 — 완료 주문건수 전달 (환불율 계산용)
        RefundSection refundSection = reportSearchRepository.aggregateRefundSection(
                storeId, fromDt, toDt, summary.totalOrderCount());

        // 3. 폐기 섹션
        WasteSection wasteSection = reportSearchRepository.aggregateWasteSection(
                storeId, fromDt, toDt);

        // 4. 입고 섹션
        StockInboundSection inboundSection = reportSearchRepository.aggregateStockInboundSection(
                storeId, from, to);

        return new ReportData(salesSection, refundSection, wasteSection, inboundSection);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(LocalDate.now())) {
            throw new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }
        if (to.isAfter(LocalDate.now())) {
            throw new AnalyticsException(AnalyticsErrorCode.FUTURE_DATE_NOT_ALLOWED);
        }
        if (from.isAfter(to)) {
            throw new AnalyticsException(AnalyticsErrorCode.INVALID_DATE_RANGE);
        }

        long daysBetween = to.toEpochDay() - from.toEpochDay();
        if (daysBetween > SalesAnalyticsConstants.MAX_QUERY_DAYS) {
            throw new AnalyticsException(AnalyticsErrorCode.DATE_RANGE_TOO_LONG);
        }
    }

    private YearMonth parseYearMonth(String yearMonth) {
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            // "YYYY-MM" 형식이 아닌 경우
            throw new AnalyticsException(AnalyticsErrorCode.INVALID_DATE_RANGE);
        }
    }
}
