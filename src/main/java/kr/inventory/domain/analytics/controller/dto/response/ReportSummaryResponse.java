package kr.inventory.domain.analytics.controller.dto.response;

import kr.inventory.domain.analytics.service.report.ReportData;
import kr.inventory.domain.analytics.service.report.SalesSection;
import kr.inventory.domain.analytics.service.report.WasteSection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportSummaryResponse(
        LocalDate from,
        LocalDate to,
        // 매출 섹션
        long totalOrderCount,
        BigDecimal totalAmount,
        BigDecimal averageOrderAmount,
        List<SalesSection.MenuEntry> menuTop5,
        // 환불 섹션
        long refundCount,
        double refundRate,
        // 폐기 섹션
        BigDecimal totalWasteAmount,
        List<WasteSection.ReasonEntry> reasonBreakdown,
        // 입고 섹션
        long totalInboundCount
) {
    public static ReportSummaryResponse from(ReportData data, LocalDate from, LocalDate to) {
        return new ReportSummaryResponse(
                from,
                to,
                data.sales().totalOrderCount(),
                data.sales().totalAmount(),
                data.sales().averageOrderAmount(),
                data.sales().menuTop5(),
                data.refund().refundCount(),
                data.refund().refundRate(),
                data.waste().totalWasteAmount(),
                data.waste().reasonBreakdown(),
                data.inbound().totalInboundCount()
        );
    }
}
