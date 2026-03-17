package kr.inventory.domain.analytics.repository;

import kr.inventory.domain.analytics.controller.dto.response.*;

import java.time.OffsetDateTime;
import java.util.List;

public interface SalesOrderSearchRepositoryCustom {

    // 일/주/월 매출 추이 (calendarInterval: "day" | "week" | "month")
    List<SalesTrendResponse> aggregateSalesTrend(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            String calendarInterval
    );

    // 요일×시간대 피크 분석
    List<SalesPeakResponse> aggregateSalesPeak(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    // 메뉴 TOP N 판매량
    List<MenuRankingResponse> aggregateMenuRanking(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            int topN
    );

    // 객단가 등 요약 지표
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
