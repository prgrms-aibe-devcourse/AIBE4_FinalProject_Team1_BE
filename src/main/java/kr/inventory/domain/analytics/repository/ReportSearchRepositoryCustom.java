package kr.inventory.domain.analytics.repository;

import kr.inventory.domain.analytics.service.report.RefundSection;
import kr.inventory.domain.analytics.service.report.StockInboundSection;
import kr.inventory.domain.analytics.service.report.WasteSection;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface ReportSearchRepositoryCustom {

    /**
     * 환불 섹션 집계
     * sales_orders 인덱스, status=REFUNDED 필터 + orderedAt 기간 필터
     */
    RefundSection aggregateRefundSection(Long storeId, OffsetDateTime from, OffsetDateTime to, long totalOrderCount);

    /**
     * 폐기 섹션 집계
     * waste_records 인덱스, wasteDate 기간 필터
     * 사유별 비율 + 폐기금액 TOP 5 식재료 포함
     */
    WasteSection aggregateWasteSection(Long storeId, OffsetDateTime from, OffsetDateTime to);

    /**
     * 입고 섹션 집계
     * stock_inbounds 인덱스, status=CONFIRMED + inboundDate 기간 필터
     * 거래처별 입고건수 포함
     */
    StockInboundSection aggregateStockInboundSection(Long storeId, LocalDate from, LocalDate to);
}
