package kr.inventory.domain.analytics.repository;

import kr.inventory.domain.analytics.controller.dto.response.*;

import java.time.OffsetDateTime;
import java.util.List;

public interface SalesOrderSearchRepositoryCustom {

    List<SalesTrendResponse> aggregateSalesTrend(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            String calendarInterval
    );

    List<SalesPeakResponse> aggregateSalesPeak(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<MenuRankingResponse> aggregateMenuRanking(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            int topN,
            String rankBy
    );

    SalesSummaryResponse aggregateSalesSummary(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    // 환불 요약 집계
    RefundSummaryResponse aggregateRefundSummary(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    // 특정 메뉴 상세 집계
    MenuSalesDetailResponse aggregateMenuSalesDetail(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            String menuName
    );
}
