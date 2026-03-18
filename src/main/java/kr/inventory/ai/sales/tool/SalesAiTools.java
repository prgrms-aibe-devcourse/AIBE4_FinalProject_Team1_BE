package kr.inventory.ai.sales.tool;

import kr.inventory.ai.common.constant.ToolDescriptionConstants;
import kr.inventory.ai.common.dto.DateRange;
import kr.inventory.ai.common.enums.DateRangePreset;
import kr.inventory.ai.common.resolver.DateRangeResolver;
import kr.inventory.ai.context.ChatToolContextProvider;
import kr.inventory.ai.context.dto.ChatToolContext;
import kr.inventory.ai.sales.constant.SalesConstants;
import kr.inventory.ai.sales.exception.SalesErrorCode;
import kr.inventory.ai.sales.exception.SalesException;
import kr.inventory.ai.sales.service.SalesAiQueryService;
import kr.inventory.ai.sales.tool.dto.request.SalesPeakToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesSummaryToolRequest;
import kr.inventory.ai.sales.tool.dto.request.SalesTrendToolRequest;
import kr.inventory.ai.sales.tool.dto.request.TopMenuRankingToolRequest;
import kr.inventory.ai.sales.tool.dto.response.SalesPeakToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesSummaryToolResponse;
import kr.inventory.ai.sales.tool.dto.response.SalesTrendToolResponse;
import kr.inventory.ai.sales.tool.dto.response.TopMenuRankingToolResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class SalesAiTools {

    private final SalesAiQueryService salesAiQueryService;
    private final ChatToolContextProvider chatToolContextProvider;
    private final DateRangeResolver dateRangeResolver;

    @Tool(
            name = "get_sales_summary",
            description = """
                    현재 사용자의 매장에 대한 총 주문 수, 총 매출액, 평균 주문 금액, 최대 주문 금액, 최소 주문 금액, 증감률을 요약합니다.
                    오늘, 어제, 이번 주, 이번 달, 최근 7일, 최근 30일, 지난달 또는 사용자 정의 날짜 범위의 매출 요약에 대한 질문에 사용하세요.
                    period 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    interval은 day, week, month 중 하나여야 합니다.
                    compareMode는 previous_period, same_period_last_week, same_period_last_month, custom 중 하나입니다.
                    compareMode가 custom이면 baseFromDate와 baseToDate를 yyyy-MM-dd 형식으로 함께 전달해야 합니다.
                    """ + ToolDescriptionConstants.DATE_RANGE_PRESET
    )
    public SalesSummaryToolResponse getSalesSummary(SalesSummaryToolRequest request) {
        validateSalesSummaryRequest(request);

        ChatToolContext context = chatToolContextProvider.getRequired();
        ResolvedSalesRange currentRange = resolveSalesSummaryRange(request);
        String compareMode = request.normalizedCompareMode();
        DateRange baseRange = resolveCustomBaseRange(request, compareMode, SalesConstants.KST);

        return salesAiQueryService.getSalesSummary(
                context.userId(),
                context.storePublicId(),
                currentRange.dateRange().from(),
                currentRange.dateRange().to(),
                request.normalizedInterval(),
                compareMode,
                currentRange.periodKey(),
                currentRange.preset(),
                baseRange != null ? baseRange.from() : null,
                baseRange != null ? baseRange.to() : null
        );
    }

    @Tool(
            name = "get_sales_trend",
            description = """
                    현재 사용자의 매장에 대해 일(day), 주(week), 월(month) 단위로 그룹화된 매출 추이 데이터를 반환합니다.
                    최근 매출 추세, 시계열 변화, 특정 기간의 매출 흐름을 묻는 질문에 사용합니다.
                    오늘, 어제, 이번 주, 이번 달, 최근 7일, 최근 30일, 지난달 또는 사용자 정의 날짜 범위를 조회할 수 있습니다.
                    period 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    interval 값은 day, week, month 중 하나여야 합니다.
                    metric 값은 amount, order_count, both 중 하나여야 합니다.
                    이 도구는 추이 포인트와 함께 최고 지점, 최저 지점, 최신 지점, 전체 변화율을 반환합니다.
                    """ + ToolDescriptionConstants.DATE_RANGE_PRESET
    )
    public SalesTrendToolResponse getSalesTrend(SalesTrendToolRequest request) {
        validateSalesTrendRequest(request);

        ChatToolContext context = chatToolContextProvider.getRequired();
        ResolvedSalesRange resolvedRange = resolveSalesTrendRange(request);

        return salesAiQueryService.getSalesTrend(
                context.userId(),
                context.storePublicId(),
                resolvedRange.dateRange().from(),
                resolvedRange.dateRange().to(),
                request.normalizedInterval(),
                request.normalizedMetric(),
                resolvedRange.periodKey(),
                resolvedRange.preset()
        );
    }

    @Tool(
            name = "get_sales_peak",
            description = """
                    현재 사용자의 매장에 대해 요일별(day-of-week) 및 시간대별(hour-of-day) 기준의 피크 매출 요약을 반환합니다.
                    피크 매출 시간대, 가장 바쁜 요일, 가장 바쁜 시간대를 묻는 질문에 사용합니다.
                    오늘, 어제, 이번 주, 이번 달, 최근 7일, 최근 30일, 지난달 또는 사용자 정의 날짜 범위를 조회할 수 있습니다.
                    period 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    viewType 값은 combined, day_only, hour_only 중 하나입니다.
                    limit는 각 요약 목록에서 몇 개의 상위 결과를 반환할지 제어합니다.
                    이 도구는 상위 시간대 목록, 상위 요일 목록, 상위 시간 목록, 최고의 요일, 최고의 시간을 반환합니다.
                    """ + ToolDescriptionConstants.DATE_RANGE_PRESET
    )
    public SalesPeakToolResponse getSalesPeak(SalesPeakToolRequest request) {
        validateSalesPeakRequest(request);

        ChatToolContext context = chatToolContextProvider.getRequired();
        ResolvedSalesRange resolvedRange = resolveSalesPeakRange(request);

        return salesAiQueryService.getSalesPeak(
                context.userId(),
                context.storePublicId(),
                resolvedRange.dateRange().from(),
                resolvedRange.dateRange().to(),
                request.resolvedLimit(),
                request.normalizedViewType(),
                resolvedRange.periodKey(),
                resolvedRange.preset()
        );
    }

    @Tool(
            name = "get_top_menu_ranking",
            description = """
                    현재 사용자의 매장에 대해 상위 메뉴 랭킹을 반환합니다.
                    베스트셀러 메뉴, 상위 메뉴 순위, 어떤 메뉴가 매출을 견인했는지 묻는 질문에 사용합니다.
                    오늘, 어제, 이번 주, 이번 달, 최근 7일, 최근 30일, 지난달 또는 사용자 정의 날짜 범위를 조회할 수 있습니다.
                    period 대신 fromDate와 toDate를 yyyy-MM-dd 형식으로 직접 전달할 수 있습니다.
                    topN은 반환할 상위 메뉴 개수를 제어합니다.
                    rankBy는 quantity 또는 amount입니다.
                    이 도구는 각 메뉴의 rank, menuName, totalQuantity, totalAmount, amountShareRate를 반환합니다.
                    """ + ToolDescriptionConstants.DATE_RANGE_PRESET
    )
    public TopMenuRankingToolResponse getTopMenuRanking(TopMenuRankingToolRequest request) {
        validateTopMenuRankingRequest(request);

        ChatToolContext context = chatToolContextProvider.getRequired();
        ResolvedSalesRange resolvedRange = resolveTopMenuRankingRange(request);

        return salesAiQueryService.getTopMenuRanking(
                context.userId(),
                context.storePublicId(),
                resolvedRange.dateRange().from(),
                resolvedRange.dateRange().to(),
                request.resolvedTopN(),
                request.normalizedRankBy(),
                resolvedRange.periodKey(),
                resolvedRange.preset()
        );
    }

    private ResolvedSalesRange resolveSalesSummaryRange(SalesSummaryToolRequest request) {
        if (request.hasExplicitDateRange()) {
            return new ResolvedSalesRange(
                    buildDateRange(request.fromDate(), request.toDate(), SalesConstants.KST),
                    "custom",
                    "custom"
            );
        }

        DateRangePreset preset = request.period() != null ? request.period() : DateRangePreset.LAST_7_DAYS;
        return new ResolvedSalesRange(
                dateRangeResolver.resolve(preset, SalesConstants.KST),
                preset.getValue(),
                preset.getValue()
        );
    }

    private ResolvedSalesRange resolveSalesTrendRange(SalesTrendToolRequest request) {
        if (request.hasExplicitDateRange()) {
            return new ResolvedSalesRange(
                    buildDateRange(request.fromDate(), request.toDate(), SalesConstants.KST),
                    "custom",
                    "custom"
            );
        }

        DateRangePreset preset = request.period() != null ? request.period() : DateRangePreset.LAST_30_DAYS;
        return new ResolvedSalesRange(
                dateRangeResolver.resolve(preset, SalesConstants.KST),
                preset.getValue(),
                preset.getValue()
        );
    }

    private ResolvedSalesRange resolveSalesPeakRange(SalesPeakToolRequest request) {
        if (request.hasExplicitDateRange()) {
            return new ResolvedSalesRange(
                    buildDateRange(request.fromDate(), request.toDate(), SalesConstants.KST),
                    "custom",
                    "custom"
            );
        }

        DateRangePreset preset = request.period() != null ? request.period() : DateRangePreset.LAST_30_DAYS;
        return new ResolvedSalesRange(
                dateRangeResolver.resolve(preset, SalesConstants.KST),
                preset.getValue(),
                preset.getValue()
        );
    }

    private ResolvedSalesRange resolveTopMenuRankingRange(TopMenuRankingToolRequest request) {
        if (request.hasExplicitDateRange()) {
            return new ResolvedSalesRange(
                    buildDateRange(request.fromDate(), request.toDate(), SalesConstants.KST),
                    "custom",
                    "custom"
            );
        }

        DateRangePreset preset = request.period() != null ? request.period() : DateRangePreset.LAST_30_DAYS;
        return new ResolvedSalesRange(
                dateRangeResolver.resolve(preset, SalesConstants.KST),
                preset.getValue(),
                preset.getValue()
        );
    }

    private DateRange resolveCustomBaseRange(
            SalesSummaryToolRequest request,
            String compareMode,
            ZoneId zoneId
    ) {
        if (!"custom".equals(compareMode)) {
            return null;
        }

        return buildDateRange(request.baseFromDate(), request.baseToDate(), zoneId);
    }

    private DateRange buildDateRange(LocalDate fromDate, LocalDate toDate, ZoneId zoneId) {
        OffsetDateTime from = fromDate.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime to = toDate.atTime(LocalTime.MAX).atZone(zoneId).toOffsetDateTime();
        return new DateRange(from, to);
    }

    private void validateSalesSummaryRequest(SalesSummaryToolRequest request) {
        if ((request.fromDate() == null) != (request.toDate() == null)) {
            throw new SalesException(SalesErrorCode.BOTH_DATES_REQUIRED);
        }

        if (request.period() != null && request.fromDate() != null) {
            throw new SalesException(SalesErrorCode.PERIOD_AND_DATES_EXCLUSIVE);
        }

        if (request.fromDate() != null && request.fromDate().isAfter(request.toDate())) {
            throw new SalesException(SalesErrorCode.INVALID_DATE_RANGE);
        }

        if ((request.baseFromDate() == null) != (request.baseToDate() == null)) {
            throw new SalesException(SalesErrorCode.BOTH_DATES_REQUIRED);
        }

        if (request.baseFromDate() != null && request.baseFromDate().isAfter(request.baseToDate())) {
            throw new SalesException(SalesErrorCode.INVALID_BASE_DATE_RANGE);
        }

        String compareMode = request.normalizedCompareMode();
        if ("custom".equals(compareMode) && (request.baseFromDate() == null || request.baseToDate() == null)) {
            throw new SalesException(SalesErrorCode.BASE_DATES_REQUIRED_FOR_CUSTOM_COMPARE);
        }

        if (!"custom".equals(compareMode) && (request.baseFromDate() != null || request.baseToDate() != null)) {
            throw new SalesException(SalesErrorCode.BASE_DATES_NOT_ALLOWED);
        }
    }

    private void validateSalesTrendRequest(SalesTrendToolRequest request) {
        if ((request.fromDate() == null) != (request.toDate() == null)) {
            throw new SalesException(SalesErrorCode.BOTH_DATES_REQUIRED);
        }

        if (request.period() != null && request.fromDate() != null) {
            throw new SalesException(SalesErrorCode.PERIOD_AND_DATES_EXCLUSIVE);
        }

        if (request.fromDate() != null && request.fromDate().isAfter(request.toDate())) {
            throw new SalesException(SalesErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateSalesPeakRequest(SalesPeakToolRequest request) {
        if ((request.fromDate() == null) != (request.toDate() == null)) {
            throw new SalesException(SalesErrorCode.BOTH_DATES_REQUIRED);
        }

        if (request.period() != null && request.fromDate() != null) {
            throw new SalesException(SalesErrorCode.PERIOD_AND_DATES_EXCLUSIVE);
        }

        if (request.fromDate() != null && request.fromDate().isAfter(request.toDate())) {
            throw new SalesException(SalesErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void validateTopMenuRankingRequest(TopMenuRankingToolRequest request) {
        if ((request.fromDate() == null) != (request.toDate() == null)) {
            throw new SalesException(SalesErrorCode.BOTH_DATES_REQUIRED);
        }

        if (request.period() != null && request.fromDate() != null) {
            throw new SalesException(SalesErrorCode.PERIOD_AND_DATES_EXCLUSIVE);
        }

        if (request.fromDate() != null && request.fromDate().isAfter(request.toDate())) {
            throw new SalesException(SalesErrorCode.INVALID_DATE_RANGE);
        }
    }

    private record ResolvedSalesRange(
            DateRange dateRange,
            String periodKey,
            String preset
    ) {
    }
}
