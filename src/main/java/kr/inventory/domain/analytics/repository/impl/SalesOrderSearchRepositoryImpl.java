package kr.inventory.domain.analytics.repository.impl;

import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.util.NamedValue;
import kr.inventory.domain.analytics.constant.SalesAnalyticsConstants;
import kr.inventory.domain.analytics.controller.dto.response.*;
import kr.inventory.domain.analytics.document.sales.SalesOrderDocument;
import kr.inventory.domain.analytics.repository.SalesOrderSearchRepositoryCustom;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SalesOrderSearchRepositoryImpl implements SalesOrderSearchRepositoryCustom {

    private final ElasticsearchOperations elasticsearchOperations;

    // ──────────────────────────────────────────────────────
    // 1. 일/주/월 매출 추이
    // ──────────────────────────────────────────────────────
    @Override
    public List<SalesTrendResponse> aggregateSalesTrend(Long storeId, OffsetDateTime from, OffsetDateTime to, String calendarInterval) {

        log.debug("Aggregating sales trend for storeId={}, from={}, to={}, interval={}",
                storeId, from, to, calendarInterval);

        // CalendarInterval 검증 및 변환
        CalendarInterval parsedInterval;
        try {
            parsedInterval = CalendarInterval.valueOf(calendarInterval);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid calendar interval: {}, using default: {}",
                    calendarInterval, SalesAnalyticsConstants.DEFAULT_CALENDAR_INTERVAL);
            parsedInterval = CalendarInterval.Day;
        }
        final CalendarInterval interval = parsedInterval;

        NativeQuery query = buildBaseQuery(storeId, from, to)
                .withAggregation(SalesAnalyticsConstants.AGG_BY_DATE, Aggregation.of(a -> a
                        .dateHistogram(dh -> dh
                                .field(SalesAnalyticsConstants.FIELD_ORDERED_AT)
                                .calendarInterval(interval)
                                .format(SalesAnalyticsConstants.DATE_FORMAT_YYYY_MM_DD)
                                .timeZone(SalesAnalyticsConstants.TIMEZONE_KST)
                        ).aggregations(SalesAnalyticsConstants.AGG_TOTAL_AMOUNT, Aggregation.of(sa -> sa
                                .sum(s -> s.field(SalesAnalyticsConstants.FIELD_TOTAL_AMOUNT))
                        ))
                ))
                .withMaxResults(0)
                .build();

        SearchHits<SalesOrderDocument> hits = elasticsearchOperations.search(query, SalesOrderDocument.class);

        List<SalesTrendResponse> result = new ArrayList<>();

        if (hits.hasAggregations()) {
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();

            DateHistogramAggregate byDate = Optional.ofNullable(aggs.get(SalesAnalyticsConstants.AGG_BY_DATE))
                    .map(agg -> agg.aggregation().getAggregate().dateHistogram())
                    .orElse(null);

            if (byDate != null) {
                for (DateHistogramBucket bucket : byDate.buckets().array()) {
                    double sum = Optional.ofNullable(bucket.aggregations().get(SalesAnalyticsConstants.AGG_TOTAL_AMOUNT))
                            .map(agg -> agg.sum().value())
                            .orElse(0.0);

                    result.add(new SalesTrendResponse(
                            bucket.keyAsString(),
                            bucket.docCount(),
                            BigDecimal.valueOf(sum).setScale(2, RoundingMode.HALF_UP)
                    ));
                }
            } else {
                log.warn("Aggregation '{}' not found in response", SalesAnalyticsConstants.AGG_BY_DATE);
            }
        }

        log.debug("Sales trend aggregation completed. Result count: {}", result.size());
        return result;
    }

    // ──────────────────────────────────────────────────────
    // 2. 요일×시간대 피크
    // ──────────────────────────────────────────────────────
    @Override
    public List<SalesPeakResponse> aggregateSalesPeak(Long storeId, OffsetDateTime from, OffsetDateTime to) {

        log.debug("Aggregating sales peak for storeId={}, from={}, to={}", storeId, from, to);

        // Painless script로 요일 추출 (1=Monday, 7=Sunday)
        Script dayScript = Script.of(s -> s
                .source(SalesAnalyticsConstants.SCRIPT_DAY_OF_WEEK)
                .lang("painless")
        );

        // Painless script로 시간 추출 (0-23)
        Script hourScript = Script.of(s -> s
                .source(SalesAnalyticsConstants.SCRIPT_HOUR_OF_DAY)
                .lang("painless")
        );

        NativeQuery query = buildBaseQuery(storeId, from, to)
                .withAggregation(SalesAnalyticsConstants.AGG_BY_DAY, Aggregation.of(a -> a
                        .terms(t -> t.script(dayScript).size(SalesAnalyticsConstants.AGGREGATION_SIZE_DAY_OF_WEEK))
                        .aggregations(SalesAnalyticsConstants.AGG_BY_HOUR, Aggregation.of(ha -> ha
                                .terms(t -> t.script(hourScript).size(SalesAnalyticsConstants.AGGREGATION_SIZE_HOUR_OF_DAY))
                        ))
                ))
                .withMaxResults(0)
                .build();

        SearchHits<SalesOrderDocument> hits =
                elasticsearchOperations.search(query, SalesOrderDocument.class);

        List<SalesPeakResponse> result = new ArrayList<>();

        if (hits.hasAggregations()) {
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();

            StringTermsAggregate byDay = Optional.ofNullable(aggs.get(SalesAnalyticsConstants.AGG_BY_DAY))
                    .map(agg -> agg.aggregation().getAggregate().sterms())
                    .orElse(null);

            if (byDay != null) {
                for (StringTermsBucket dayBucket : byDay.buckets().array()) {
                    int day = Integer.parseInt(dayBucket.key().stringValue());

                    StringTermsAggregate byHour = Optional.ofNullable(
                            dayBucket.aggregations().get(SalesAnalyticsConstants.AGG_BY_HOUR))
                            .map(agg -> agg.sterms())
                            .orElse(null);

                    if (byHour != null) {
                        for (StringTermsBucket hourBucket : byHour.buckets().array()) {
                            int hour = Integer.parseInt(hourBucket.key().stringValue());
                            result.add(new SalesPeakResponse(day, hour, hourBucket.docCount()));
                        }
                    }
                }
            } else {
                log.warn("Aggregation '{}' not found in response", SalesAnalyticsConstants.AGG_BY_DAY);
            }
        }

        log.debug("Sales peak aggregation completed. Result count: {}", result.size());
        return result;
    }

    // ──────────────────────────────────────────────────────
    // 3. 메뉴 TOP N
    // ──────────────────────────────────────────────────────
    @Override
    public List<MenuRankingResponse> aggregateMenuRanking(
            Long storeId, OffsetDateTime from, OffsetDateTime to, int topN) {

        log.debug("Aggregating menu ranking for storeId={}, from={}, to={}, topN={}",
                storeId, from, to, topN);

        NativeQuery query = buildBaseQuery(storeId, from, to)
                .withAggregation(SalesAnalyticsConstants.AGG_BY_MENU, Aggregation.of(a -> a
                        .nested(n -> n.path(SalesAnalyticsConstants.FIELD_ITEMS))
                        .aggregations(SalesAnalyticsConstants.AGG_MENU_NAME, Aggregation.of(ma -> ma
                                .terms(t -> t
                                        .field(SalesAnalyticsConstants.FIELD_ITEMS_MENU_NAME)
                                        .size(topN)
                                        .order(NamedValue.of(SalesAnalyticsConstants.AGG_TOTAL_QUANTITY, SortOrder.Desc))
                                ).aggregations(SalesAnalyticsConstants.AGG_TOTAL_QUANTITY, Aggregation.of(qa -> qa
                                        .sum(s -> s.field(SalesAnalyticsConstants.FIELD_ITEMS_QUANTITY))
                                )).aggregations(SalesAnalyticsConstants.AGG_TOTAL_AMOUNT, Aggregation.of(sa -> sa
                                        .sum(s -> s.field(SalesAnalyticsConstants.FIELD_ITEMS_SUBTOTAL))
                                ))
                        ))
                ))
                .withMaxResults(0)
                .build();

        SearchHits<SalesOrderDocument> hits =
                elasticsearchOperations.search(query, SalesOrderDocument.class);

        List<MenuRankingResponse> result = new ArrayList<>();

        if (hits.hasAggregations()) {
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();

            NestedAggregate byMenu = Optional.ofNullable(aggs.get(SalesAnalyticsConstants.AGG_BY_MENU))
                    .map(agg -> agg.aggregation().getAggregate().nested())
                    .orElse(null);

            if (byMenu != null) {
                StringTermsAggregate menuName = Optional.ofNullable(
                        byMenu.aggregations().get(SalesAnalyticsConstants.AGG_MENU_NAME))
                        .map(agg -> agg.sterms())
                        .orElse(null);

                if (menuName != null) {
                    int rank = 1;
                    for (StringTermsBucket bucket : menuName.buckets().array()) {
                        double qty = Optional.ofNullable(
                                bucket.aggregations().get(SalesAnalyticsConstants.AGG_TOTAL_QUANTITY))
                                .map(agg -> agg.sum().value())
                                .orElse(0.0);

                        double amount = Optional.ofNullable(
                                bucket.aggregations().get(SalesAnalyticsConstants.AGG_TOTAL_AMOUNT))
                                .map(agg -> agg.sum().value())
                                .orElse(0.0);

                        result.add(new MenuRankingResponse(
                                rank++,
                                bucket.key().stringValue(),
                                (long) qty,
                                BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
                        ));
                    }
                }
            } else {
                log.warn("Aggregation '{}' not found in response", SalesAnalyticsConstants.AGG_BY_MENU);
            }
        }

        log.debug("Menu ranking aggregation completed. Result count: {}", result.size());
        return result;
    }

    // ──────────────────────────────────────────────────────
    // 4. 매출 요약 (객단가 등)
    // ──────────────────────────────────────────────────────
    @Override
    public SalesSummaryResponse aggregateSalesSummary(Long storeId, OffsetDateTime from, OffsetDateTime to) {

        log.debug("Aggregating sales summary for storeId={}, from={}, to={}", storeId, from, to);

        NativeQuery query = buildBaseQuery(storeId, from, to)
                .withAggregation(SalesAnalyticsConstants.AGG_TOTAL_AMOUNT,
                        Aggregation.of(a -> a.sum(s -> s.field(SalesAnalyticsConstants.FIELD_TOTAL_AMOUNT))))
                .withAggregation(SalesAnalyticsConstants.AGG_AVG_AMOUNT,
                        Aggregation.of(a -> a.avg(s -> s.field(SalesAnalyticsConstants.FIELD_TOTAL_AMOUNT))))
                .withAggregation(SalesAnalyticsConstants.AGG_MAX_AMOUNT,
                        Aggregation.of(a -> a.max(s -> s.field(SalesAnalyticsConstants.FIELD_TOTAL_AMOUNT))))
                .withAggregation(SalesAnalyticsConstants.AGG_MIN_AMOUNT,
                        Aggregation.of(a -> a.min(s -> s.field(SalesAnalyticsConstants.FIELD_TOTAL_AMOUNT))))
                .withMaxResults(0)
                .build();

        SearchHits<SalesOrderDocument> hits =
                elasticsearchOperations.search(query, SalesOrderDocument.class);

        // 데이터가 없는 경우 빈 응답 반환
        if (!hits.hasAggregations() || hits.getTotalHits() == 0) {
            log.debug("No sales data found for the given period");
            return new SalesSummaryResponse(0L, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null);
        }

        ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();

        double total = Optional.ofNullable(aggs.get(SalesAnalyticsConstants.AGG_TOTAL_AMOUNT))
                .map(agg -> agg.aggregation().getAggregate().sum().value())
                .orElse(0.0);

        double avg = Optional.ofNullable(aggs.get(SalesAnalyticsConstants.AGG_AVG_AMOUNT))
                .map(agg -> agg.aggregation().getAggregate().avg().value())
                .orElse(0.0);

        double max = Optional.ofNullable(aggs.get(SalesAnalyticsConstants.AGG_MAX_AMOUNT))
                .map(agg -> agg.aggregation().getAggregate().max().value())
                .orElse(0.0);

        double min = Optional.ofNullable(aggs.get(SalesAnalyticsConstants.AGG_MIN_AMOUNT))
                .map(agg -> agg.aggregation().getAggregate().min().value())
                .orElse(0.0);

        // NaN/Infinity 처리
        avg = Double.isNaN(avg) ? 0.0 : avg;
        max = (Double.isInfinite(max) || max == Double.NEGATIVE_INFINITY) ? 0.0 : max;
        min = (Double.isInfinite(min) || min == Double.POSITIVE_INFINITY) ? 0.0 : min;

        SalesSummaryResponse response = new SalesSummaryResponse(
                hits.getTotalHits(),
                BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(max).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(min).setScale(2, RoundingMode.HALF_UP),
                null, null, null, null
        );

        log.debug("Sales summary aggregation completed: {}", response);
        return response;
    }

    // ──────────────────────────────────────────────────────
    // 5. 환불 요약 집계
    // ──────────────────────────────────────────────────────
    @Override
    public RefundSummaryResponse aggregateRefundSummary(Long storeId, OffsetDateTime from, OffsetDateTime to) {

        log.debug("Aggregating refund summary for storeId={}, from={}, to={}", storeId, from, to);

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.term(t -> t
                                .field(SalesAnalyticsConstants.FIELD_STORE_ID)
                                .value(storeId)))
                        .filter(f -> f.range(r -> r.date(d -> d
                                .field(SalesAnalyticsConstants.FIELD_ORDERED_AT)
                                .gte(from.format(SalesAnalyticsConstants.ES_DATE_FORMATTER))
                                .lte(to.format(SalesAnalyticsConstants.ES_DATE_FORMATTER)))))
                ))
                .withAggregation(SalesAnalyticsConstants.AGG_BY_STATUS, Aggregation.of(a -> a
                        .terms(t -> t
                                .field(SalesAnalyticsConstants.FIELD_STATUS)
                                .size(2)
                        )
                        .aggregations(SalesAnalyticsConstants.AGG_TOTAL_AMOUNT, Aggregation.of(sa -> sa
                                .sum(s -> s.field(SalesAnalyticsConstants.FIELD_TOTAL_AMOUNT))
                        ))
                ))
                .withMaxResults(0)
                .build();

        SearchHits<SalesOrderDocument> hits =
                elasticsearchOperations.search(query, SalesOrderDocument.class);

        // 파싱
        ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();

        StringTermsAggregate byStatus = Optional.ofNullable(aggs.get(SalesAnalyticsConstants.AGG_BY_STATUS))
                .map(agg -> agg.aggregation().getAggregate().sterms())
                .orElse(null);

        long completedCount = 0L;
        long refundCount    = 0L;
        double refundAmount = 0.0;

        if (byStatus != null) {
            for (StringTermsBucket bucket : byStatus.buckets().array()) {
                String status = bucket.key().stringValue();
                if (SalesOrderStatus.COMPLETED.name().equals(status)) {
                    completedCount = bucket.docCount();
                } else if (SalesOrderStatus.REFUNDED.name().equals(status)) {
                    refundCount = bucket.docCount();
                    refundAmount = Optional.ofNullable(
                                    bucket.aggregations().get(SalesAnalyticsConstants.AGG_TOTAL_AMOUNT))
                            .map(agg -> agg.sum().value())
                            .orElse(0.0);
                }
            }
        }

        long totalCount = completedCount + refundCount;
        double refundRate = totalCount > 0 ? Math.round((refundCount * 100.0 / totalCount) * 10.0) / 10.0 : 0.0;

        return new RefundSummaryResponse(
                refundCount,
                BigDecimal.valueOf(refundAmount).setScale(2, RoundingMode.HALF_UP),
                refundRate
        );
    }

    // ──────────────────────────────────────────────────────
    // 6. 특정 메뉴 상세 집계
    // ──────────────────────────────────────────────────────
    @Override
    public MenuSalesDetailResponse aggregateMenuSalesDetail(
            Long storeId, OffsetDateTime from, OffsetDateTime to, String menuName) {

        log.debug("Aggregating menu sales detail for storeId={}, menuName={}", storeId, menuName);

        // 한 번의 쿼리로 전체 매출 + 특정 메뉴 매출 조회
        NativeQuery query = buildBaseQuery(storeId, from, to)
                // 전체 매출 합계
                .withAggregation(SalesAnalyticsConstants.AGG_TOTAL_SALES_AMOUNT, Aggregation.of(a -> a
                        .sum(s -> s.field(SalesAnalyticsConstants.FIELD_TOTAL_AMOUNT))
                ))
                // 특정 메뉴 집계
                .withAggregation(SalesAnalyticsConstants.AGG_BY_MENU, Aggregation.of(a -> a
                        .nested(n -> n.path(SalesAnalyticsConstants.FIELD_ITEMS))
                        .aggregations(SalesAnalyticsConstants.AGG_MENU_FILTER, Aggregation.of(fa -> fa
                                .filter(f -> f.term(t -> t
                                        .field(SalesAnalyticsConstants.FIELD_ITEMS_MENU_NAME)
                                        .value(menuName)))
                                .aggregations(SalesAnalyticsConstants.AGG_TOTAL_QUANTITY, Aggregation.of(qa -> qa
                                        .sum(s -> s.field(SalesAnalyticsConstants.FIELD_ITEMS_QUANTITY))
                                ))
                                .aggregations(SalesAnalyticsConstants.AGG_TOTAL_AMOUNT, Aggregation.of(sa -> sa
                                        .sum(s -> s.field(SalesAnalyticsConstants.FIELD_ITEMS_SUBTOTAL))
                                ))
                        ))
                ))
                .withMaxResults(0)
                .build();

        SearchHits<SalesOrderDocument> hits =
                elasticsearchOperations.search(query, SalesOrderDocument.class);

        if (!hits.hasAggregations()) {
            log.debug("No aggregations found for menuName={}", menuName);
            return new MenuSalesDetailResponse(menuName, 0L, BigDecimal.ZERO, BigDecimal.ZERO, 0.0);
        }

        ElasticsearchAggregations aggs = (ElasticsearchAggregations) hits.getAggregations();

        // 전체 매출 합계 추출
        double totalSalesAmountRaw = Optional.ofNullable(aggs.get(SalesAnalyticsConstants.AGG_TOTAL_SALES_AMOUNT))
                .map(agg -> agg.aggregation().getAggregate().sum().value())
                .orElse(0.0);
        BigDecimal totalSalesAmount = BigDecimal.valueOf(totalSalesAmountRaw).setScale(2, RoundingMode.HALF_UP);

        // 특정 메뉴 집계 추출
        NestedAggregate byMenu = Optional.ofNullable(aggs.get(SalesAnalyticsConstants.AGG_BY_MENU))
                .map(agg -> agg.aggregation().getAggregate().nested())
                .orElse(null);

        if (byMenu == null) {
            return new MenuSalesDetailResponse(menuName, 0L, BigDecimal.ZERO, BigDecimal.ZERO, 0.0);
        }

        // filter aggregation 결과 추출
        FilterAggregate menuFilter = Optional.ofNullable(
                        byMenu.aggregations().get(SalesAnalyticsConstants.AGG_MENU_FILTER))
                .map(agg -> agg.filter())
                .orElse(null);

        if (menuFilter == null || menuFilter.docCount() == 0) {
            log.debug("No data found for menuName={}", menuName);
            return new MenuSalesDetailResponse(menuName, 0L, BigDecimal.ZERO, BigDecimal.ZERO, 0.0);
        }

        double qty = Optional.ofNullable(
                        menuFilter.aggregations().get(SalesAnalyticsConstants.AGG_TOTAL_QUANTITY))
                .map(agg -> agg.sum().value())
                .orElse(0.0);

        double amount = Optional.ofNullable(
                        menuFilter.aggregations().get(SalesAnalyticsConstants.AGG_TOTAL_AMOUNT))
                .map(agg -> agg.sum().value())
                .orElse(0.0);

        BigDecimal totalAmount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        long totalQuantity = (long) qty;

        // 평균 판매단가
        BigDecimal avgSellingPrice = totalQuantity > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 전체 매출 대비 비율
        double shareRate = 0.0;
        if (totalSalesAmount != null && totalSalesAmount.compareTo(BigDecimal.ZERO) > 0) {
            shareRate = totalAmount
                    .divide(totalSalesAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            shareRate = Math.round(shareRate * 10.0) / 10.0;
        }

        log.debug("Menu sales detail aggregation completed. menuName={} qty={} amount={}",
                menuName, totalQuantity, totalAmount);
        return new MenuSalesDetailResponse(menuName, totalQuantity, totalAmount, avgSellingPrice, shareRate);
    }


    // ──────────────────────────────────────────────────────
    // Private Helper Methods
    // ──────────────────────────────────────────────────────

    /**
     * 공통 쿼리 빌더 생성
     * - storeId 필터
     * - COMPLETED 상태 필터
     * - completedAt 기간 필터
     */
    private NativeQueryBuilder buildBaseQuery(
            Long storeId, OffsetDateTime from, OffsetDateTime to) {

        return NativeQuery.builder()
            .withQuery(q -> q
                    .bool(b -> b
                            .filter(f -> f.term(t -> t
                                    .field(SalesAnalyticsConstants.FIELD_STORE_ID)
                                    .value(storeId)))
                            .filter(f -> f.term(t -> t
                                    .field(SalesAnalyticsConstants.FIELD_STATUS)
                                    .value(SalesOrderStatus.COMPLETED.name())))
                            .filter(f -> f.range(r -> r.date(d -> d
                                    .field(SalesAnalyticsConstants.FIELD_ORDERED_AT)
                                    .gte(from.format(SalesAnalyticsConstants.ES_DATE_FORMATTER))
                                    .lte(to.format(SalesAnalyticsConstants.ES_DATE_FORMATTER)))))
                    )
            );
    }
}