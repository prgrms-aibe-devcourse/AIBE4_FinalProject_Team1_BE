package kr.inventory.ai.sales.tool;

import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.ai.sales.service.SalesAiQueryService;
import kr.inventory.ai.sales.tool.dto.request.MenuSalesDetailToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesOrderDetailToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesPeakToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesRecordsToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesRefundSummaryToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesSummaryToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesTrendToolRequest;
import kr.inventory.ai.sales.tool.dto.request.TopMenuRankingToolRequest;
import kr.inventory.ai.sales.tool.dto.response.MenuSalesDetailToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesOrderDetailToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesPeakToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesRecordsToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesRefundSummaryToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesSummaryToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesTrendToolResponse;
import kr.inventory.ai.sales.tool.dto.response.TopMenuRankingToolResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesAiTools {

    private final SalesAiQueryService salesAiQueryService;
    private final ChatToolContextProvider chatToolContextProvider;

    @Tool(
            name = "get_sales_summary",
            description = """
                    현재 사용자의 매장에 대한 총 주문 수, 총 매출액, 평균 주문 금액, 최대 주문 금액, 최소 주문 금액, 증감률을 요약합니다.
                    오늘, 어제, 이번 주, 이번 달, 최근 7일, 최근 30일, 지난달 또는 사용자 정의 날짜 범위의 매출 요약에 대한 질문에 사용하세요.
                    사용 가능한 preset: today, yesterday, this_week, this_month, last_7_days, last_30_days, last_month.
                    preset 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    interval은 day, week, month 중 하나여야 합니다.
                    compareMode는 previous_period, same_period_last_week, same_period_last_month, custom 중 하나입니다.
                    compareMode가 custom이면 baseFromDate와 baseToDate를 yyyy-MM-dd 형식으로 함께 전달해야 합니다.
                    """
    )
    public SalesSummaryToolResponse getSalesSummary(SalesSummaryToolRequest request) {
        ChatToolContext context = chatToolContextProvider.getRequired();
        return salesAiQueryService.getSalesSummary(context.userId(), context.storePublicId(), request);
    }

    @Tool(
            name = "get_sales_trend",
            description = """
                    현재 사용자의 매장에 대해 일(day), 주(week), 월(month) 단위로 그룹화된 매출 추이 데이터를 반환합니다.
                    최근 매출 추세, 시계열 변화, 특정 기간의 매출 흐름을 묻는 질문에 사용합니다.
                    사용 가능한 preset: today, yesterday, this_week, this_month, last_7_days, last_30_days, last_month.
                    preset 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    interval 값은 day, week, month 중 하나여야 합니다.
                    metric 값은 amount, order_count, both 중 하나여야 합니다.
                    이 도구는 추이 포인트와 함께 최고 지점, 최저 지점, 최신 지점, 전체 변화율을 반환합니다.
                    """
    )
    public SalesTrendToolResponse getSalesTrend(SalesTrendToolRequest request) {
        ChatToolContext context = chatToolContextProvider.getRequired();
        return salesAiQueryService.getSalesTrend(context.userId(), context.storePublicId(), request);
    }

    @Tool(
            name = "get_sales_peak",
            description = """
                    현재 사용자의 매장에 대해 요일별(day-of-week) 및 시간대별(hour-of-day) 기준의 피크 매출 요약을 반환합니다.
                    피크 매출 시간대, 가장 바쁜 요일, 가장 바쁜 시간대를 묻는 질문에 사용합니다.
                    사용 가능한 preset: today, yesterday, this_week, this_month, last_7_days, last_30_days, last_month.
                    preset 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    viewType 값은 combined, day_only, hour_only 중 하나입니다.
                    limit는 각 요약 목록에서 몇 개의 상위 결과를 반환할지 제어합니다.
                    이 도구는 상위 시간대 목록, 상위 요일 목록, 상위 시간 목록, 최고의 요일, 최고의 시간을 반환합니다.
                    """
    )
    public SalesPeakToolResponse getSalesPeak(SalesPeakToolRequest request) {
        ChatToolContext context = chatToolContextProvider.getRequired();
        return salesAiQueryService.getSalesPeak(context.userId(), context.storePublicId(), request);
    }

    @Tool(
            name = "get_top_menu_ranking",
            description = """
                    현재 사용자의 매장에 대해 상위 메뉴 랭킹을 반환합니다.
                    베스트셀러 메뉴, 상위 메뉴 순위, 어떤 메뉴가 매출을 견인했는지 묻는 질문에 사용합니다.
                    사용 가능한 preset: today, yesterday, this_week, this_month, last_7_days, last_30_days, last_month.
                    preset 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    topN은 반환할 상위 메뉴 개수를 제어합니다.
                    rankBy는 quantity 또는 amount입니다.
                    이 도구는 각 메뉴의 rank, menuName, totalQuantity, totalAmount, amountShareRate를 반환합니다.
                    """
    )
    public TopMenuRankingToolResponse getTopMenuRanking(TopMenuRankingToolRequest request) {
        ChatToolContext context = chatToolContextProvider.getRequired();
        return salesAiQueryService.getTopMenuRanking(context.userId(), context.storePublicId(), request);
    }

    @Tool(
            name = "get_sales_records",
            description = """
                    현재 사용자의 매장에서 RDS 기반 기간별 매출 기록 목록을 조회합니다.
                    주문 내역, 환불 주문 목록, 완료된 주문, 홀 주문, 포장 주문 또는 요약과 함께 매출 기록을 원할 때 사용합니다.
                    "환불 주문 보여줘", "환불 주문만 보여줘", "환불된 주문 목록 보여줘" 같은 질문은 이 도구를 사용해야 하며,
                    status를 REFUNDED로 설정해 주문 목록을 반환해야 합니다.
                    반대로 환불 건수/환불 금액/환불률처럼 요약 수치만 묻는 경우에는 get_refund_summary를 사용하세요.
                    사용자가 직전 대화에서 이미 기간을 정했고 후속으로 "환불 주문만 보여줘"처럼 다시 말하면,
                    가능한 경우 직전과 같은 기간을 유지한 채 status만 REFUNDED로 적용하세요.
                    사용 가능한 preset: today, yesterday, this_week, this_month, last_7_days, last_30_days, last_month.
                    preset 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    선택적 필터:
                    - status: COMPLETED 또는 REFUNDED
                    - type: DINE_IN 또는 TAKEOUT
                    - menuName: 특정 메뉴 이름을 포함하는 주문 필터링
                    - amountMin / amountMax: 주문 총액 범위로 필터링
                    - tableCode: 테이블 코드로 주문 필터링
                    - sortBy: ordered_at_desc, ordered_at_asc, total_amount_desc, total_amount_asc
                    - page: 1부터 시작하는 페이지 번호
                    - size: 페이지 크기
                    특정 기간의 가장 큰 주문을 찾을 때는 total_amount_desc와 size=1을 사용하세요.
                    """
    )
    public SalesRecordsToolResponse getSalesRecords(SalesRecordsToolRequest request) {
        ChatToolContext context = chatToolContextProvider.getRequired();
        return salesAiQueryService.getSalesRecords(context.userId(), context.storePublicId(), request);
    }

    @Tool(
            name = "get_sales_order_detail",
            description = """
                    현재 사용자의 매장에서 특정 주문 1건의 상세 매출 내역을 조회합니다.
                    우선적으로 orderPublicId를 입력으로 사용합니다.
                    orderPublicId가 없는 경우, 검색 조건을 사용하여 RDS에서 일치하는 단일 주문을 찾을 수도 있습니다.
                    사용 가능한 preset: today, yesterday, this_week, this_month, last_7_days, last_30_days, last_month.
                    preset 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    선택적 검색 필터:
                    - status: COMPLETED 또는 REFUNDED
                    - type: DINE_IN 또는 TAKEOUT
                    - menuName: 특정 메뉴 이름을 포함하는 주문 찾기
                    - amountMin / amountMax: 총액 범위 내의 주문 찾기
                    - tableCode: 특정 테이블의 주문 찾기
                    - sortBy: ordered_at_desc, ordered_at_asc, total_amount_desc, total_amount_asc
                    - selectionIndex: 정렬 및 필터링 후 1부터 시작하는 위치
                    예시:
                    - 오늘 가장 큰 주문 상세 -> preset=today, sortBy=total_amount_desc, selectionIndex=1
                    - 최근 기간에서 가장 큰 환불 주문 -> preset=last_7_days, status=REFUNDED, sortBy=total_amount_desc, selectionIndex=1
                    - 특정 기간의 첫 번째 주문 -> fromDate/toDate와 sortBy=ordered_at_asc, selectionIndex=1
                    이 도구는 주문 상태, 주문 유형, 주문/완료/환불 시각, 테이블 코드, 총액, 환불액, 순매출,
                    그리고 메뉴명, 단가, 수량, 소계를 포함한 주문 항목을 반환합니다.
                    """
    )
    public SalesOrderDetailToolResponse getSalesOrderDetail(SalesOrderDetailToolRequest request) {
        ChatToolContext context = chatToolContextProvider.getRequired();
        return salesAiQueryService.getSalesOrderDetail(context.userId(), context.storePublicId(), request);
    }

    @Tool(
            name = "get_refund_summary",
            description = """
                    현재 사용자의 매장에 대해 ES 기반 환불 요약을 반환합니다.
                    환불 건수, 총 환불 금액, 환불률, 순매출 영향도를 묻는 질문에 사용합니다.
                    이 도구는 요약 전용입니다.
                    "환불 주문 보여줘", "환불 주문만 보여줘", "환불된 주문 목록"처럼 주문 목록 자체를 보여달라는 질문에는 사용하지 말고,
                    반드시 get_sales_records를 status=REFUNDED로 호출하세요.
                    사용 가능한 preset: today, yesterday, this_week, this_month, last_7_days, last_30_days, last_month.
                    preset 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    """
    )
    public SalesRefundSummaryToolResponse getRefundSummary(SalesRefundSummaryToolRequest request) {
        ChatToolContext context = chatToolContextProvider.getRequired();
        return salesAiQueryService.getRefundSummary(context.userId(), context.storePublicId(), request);
    }

    @Tool(
            name = "get_menu_sales_detail",
            description = """
                    현재 사용자의 매장에 대해 특정 메뉴 하나의 기간별 매출 상세를 반환합니다.
                    메뉴별 판매 수량, 총매출, 평균 판매단가, 전체 매출 대비 비중을 묻는 질문에 사용합니다.
                    menuName은 필수입니다.
                    사용 가능한 preset: today, yesterday, this_week, this_month, last_7_days, last_30_days, last_month.
                    preset 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    """
    )
    public MenuSalesDetailToolResponse getMenuSalesDetail(MenuSalesDetailToolRequest request) {
        ChatToolContext context = chatToolContextProvider.getRequired();
        return salesAiQueryService.getMenuSalesDetail(context.userId(), context.storePublicId(), request);
    }
}
