package kr.inventory.domain.analytics.repository.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.NamedValue;
import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.analytics.constant.ReportConstants;
import kr.inventory.domain.analytics.constant.SalesAnalyticsConstants;
import kr.inventory.domain.analytics.document.sales.SalesOrderDocument;
import kr.inventory.domain.analytics.document.stock.StockInboundDocument;
import kr.inventory.domain.analytics.document.stock.WasteRecordDocument;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.repository.ReportSearchRepositoryCustom;
import kr.inventory.domain.analytics.service.report.RefundSection;
import kr.inventory.domain.analytics.service.report.StockInboundSection;
import kr.inventory.domain.analytics.service.report.WasteSection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ReportSearchRepositoryImpl implements ReportSearchRepositoryCustom {

    private final ElasticsearchClient elasticsearchClient;

    // ──────────────────────────────────────────────────────
    // 1. 환불 섹션
    // ──────────────────────────────────────────────────────
    @Override
    public RefundSection aggregateRefundSection(Long storeId, OffsetDateTime from, OffsetDateTime to, long totalOrderCount) {
        try {
            SearchResponse<SalesOrderDocument> response =
                    elasticsearchClient.search(s -> s
                            .index(ElasticsearchIndex.SALES_ORDERS)
                            .size(0)
                            .trackTotalHits(t -> t.enabled(true))
                            .query(q -> q.bool(b -> b
                                    .filter(f -> f.term(t -> t
                                            .field(SalesAnalyticsConstants.FIELD_STORE_ID)
                                            .value(storeId)))
                                    .filter(f -> f.term(t -> t
                                            .field(SalesAnalyticsConstants.FIELD_STATUS)
                                            .value(ReportConstants.STATUS_REFUNDED)))
                                    .filter(f -> f.range(r -> r.untyped(u -> u
                                            .field(SalesAnalyticsConstants.FIELD_ORDERED_AT)
                                            .gte(JsonData.of(from.format(SalesAnalyticsConstants.ES_DATE_FORMATTER)))
                                            .lte(JsonData.of(to.format(SalesAnalyticsConstants.ES_DATE_FORMATTER))))))
                            ))
                            .aggregations(ReportConstants.AGG_TOTAL_REFUND_AMOUNT, a -> a
                                    .sum(sm -> sm.field(SalesAnalyticsConstants.FIELD_TOTAL_AMOUNT))
                            ),
                    SalesOrderDocument.class);

            long refundCount = response.hits().total() != null
                    ? response.hits().total().value() : 0L;

            double totalRefundAmountRaw = Optional.ofNullable(
                            response.aggregations().get(ReportConstants.AGG_TOTAL_REFUND_AMOUNT))
                    .map(agg -> agg.sum().value())
                    .orElse(0.0);

            // 환불률 계산
            double refundRate = totalOrderCount > 0 ? (refundCount * 100.0) / totalOrderCount : 0.0;

            log.debug("[Report] 환불 집계 완료 storeId={} refundCount={}", storeId, refundCount);
            return new RefundSection(
                    refundCount,
                    BigDecimal.valueOf(totalRefundAmountRaw).setScale(2, RoundingMode.HALF_UP),
                    Math.round(refundRate * 10.0) / 10.0
            );
        } catch (IOException e) {
            log.error("[Report] 환불 섹션 집계 실패 storeId={}", storeId, e);
            // NOTE: AnalyticsErrorCode.REPORT_GENERATION_FAILED enum 추가 필요
            throw new AnalyticsException(AnalyticsErrorCode.REPORT_GENERATION_FAILED);
        }
    }

    // ──────────────────────────────────────────────────────
    // 2. 폐기 섹션
    // ──────────────────────────────────────────────────────
    @Override
    public WasteSection aggregateWasteSection(Long storeId, OffsetDateTime from, OffsetDateTime to) {
        try {
            SearchResponse<WasteRecordDocument> response =
                    elasticsearchClient.search(s -> s
                            .index(ElasticsearchIndex.WASTE_RECORDS)
                            .size(0)
                            .trackTotalHits(t -> t.enabled(true))
                            .query(q -> q.bool(b -> b
                                    .filter(f -> f.term(t -> t
                                            .field(SalesAnalyticsConstants.FIELD_STORE_ID)
                                            .value(storeId)))
                                    .filter(f -> f.range(r -> r.untyped(u -> u
                                            .field(ReportConstants.FIELD_WASTE_DATE)
                                            .gte(JsonData.of(from.format(SalesAnalyticsConstants.ES_DATE_FORMATTER)))
                                            .lte(JsonData.of(to.format(SalesAnalyticsConstants.ES_DATE_FORMATTER))))))
                            ))
                            // 전체 합계
                            .aggregations(ReportConstants.AGG_SUM_WASTE_AMOUNT, a -> a
                                    .sum(sm -> sm.field(ReportConstants.FIELD_WASTE_AMOUNT)))
                            .aggregations(ReportConstants.AGG_SUM_WASTE_QUANTITY, a -> a
                                    .sum(sm -> sm.field(ReportConstants.FIELD_WASTE_QUANTITY)))
                            // 사유별 집계
                            .aggregations(ReportConstants.AGG_BY_REASON, a -> a
                                    .terms(t -> t
                                            .field(ReportConstants.FIELD_WASTE_REASON)
                                            .size(ReportConstants.REPORT_REASON_SIZE))
                                    .aggregations(ReportConstants.AGG_SUM_WASTE_AMOUNT, sa -> sa
                                            .sum(sm -> sm.field(ReportConstants.FIELD_WASTE_AMOUNT)))
                            )
                            // 식재료별 TOP5 (wasteAmount 기준 내림차순)
                            .aggregations(ReportConstants.AGG_BY_INGREDIENT, a -> a
                                    .terms(t -> t
                                            .field(ReportConstants.FIELD_INGREDIENT_ID)
                                            .size(ReportConstants.REPORT_TOP_N_WASTE_INGREDIENT)
                                            .order(NamedValue.of(
                                                    ReportConstants.AGG_SUM_WASTE_AMOUNT, SortOrder.Desc)))
                                    .aggregations(ReportConstants.AGG_SUM_WASTE_AMOUNT, sa -> sa
                                            .sum(sm -> sm.field(ReportConstants.FIELD_WASTE_AMOUNT)))
                                    .aggregations(ReportConstants.AGG_SUM_WASTE_QUANTITY, sa -> sa
                                            .sum(sm -> sm.field(ReportConstants.FIELD_WASTE_QUANTITY)))
                                    .aggregations(ReportConstants.AGG_TOP_HIT, sa -> sa
                                            .topHits(th -> th.size(1)))
                            ),
                    WasteRecordDocument.class);

            long totalWasteCount = response.hits().total() != null
                    ? response.hits().total().value() : 0L;

            double totalAmountRaw = Optional.ofNullable(
                            response.aggregations().get(ReportConstants.AGG_SUM_WASTE_AMOUNT))
                    .map(agg -> agg.sum().value())
                    .orElse(0.0);
            double totalQtyRaw = Optional.ofNullable(
                            response.aggregations().get(ReportConstants.AGG_SUM_WASTE_QUANTITY))
                    .map(agg -> agg.sum().value())
                    .orElse(0.0);

            List<WasteSection.ReasonEntry> reasonBreakdown = parseReasonBreakdown(response, totalWasteCount);
            List<WasteSection.IngredientEntry> top5Ingredients = parseTop5Ingredients(response);

            log.debug("[Report] 폐기 집계 완료 storeId={} totalWasteCount={}", storeId, totalWasteCount);
            return new WasteSection(
                    BigDecimal.valueOf(totalAmountRaw).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(totalQtyRaw).setScale(3, RoundingMode.HALF_UP),
                    reasonBreakdown,
                    top5Ingredients
            );
        } catch (IOException e) {
            log.error("[Report] 폐기 섹션 집계 실패 storeId={}", storeId, e);
            throw new AnalyticsException(AnalyticsErrorCode.REPORT_GENERATION_FAILED);
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. 입고 섹션
    // ──────────────────────────────────────────────────────
    @Override
    public StockInboundSection aggregateStockInboundSection(Long storeId, LocalDate from, LocalDate to) {
        try {
            SearchResponse<StockInboundDocument> response =
                    elasticsearchClient.search(s -> s
                            .index(ElasticsearchIndex.STOCK_INBOUNDS)
                            .size(0)
                            .trackTotalHits(t -> t.enabled(true))
                            .query(q -> q.bool(b -> b
                                    .filter(f -> f.term(t -> t
                                            .field(SalesAnalyticsConstants.FIELD_STORE_ID)
                                            .value(storeId)))
                                    .filter(f -> f.term(t -> t
                                            .field(SalesAnalyticsConstants.FIELD_STATUS)
                                            .value(ReportConstants.STATUS_CONFIRMED)))
                                    // inboundDate는 LocalDate(date 포맷) → yyyy-MM-dd 문자열 사용
                                    .filter(f -> f.range(r -> r.untyped(u -> u
                                            .field(ReportConstants.FIELD_INBOUND_DATE)
                                            .gte(JsonData.of(from.toString()))
                                            .lte(JsonData.of(to.toString())))))
                            ))
                            .aggregations(ReportConstants.AGG_BY_VENDOR, a -> a
                                    .terms(t -> t
                                            .field(ReportConstants.FIELD_VENDOR_NAME)
                                            .size(ReportConstants.REPORT_VENDOR_SIZE)
                                            .missing(ReportConstants.DEFAULT_UNKNOWN_VENDOR)
                                    )
                            ),
                    StockInboundDocument.class);

            long totalInboundCount = response.hits().total() != null
                    ? response.hits().total().value() : 0L;

            List<StockInboundSection.VendorEntry> vendorBreakdown = parseVendorBreakdown(response);

            log.debug("[Report] 입고 집계 완료 storeId={} totalInboundCount={}", storeId, totalInboundCount);
            return new StockInboundSection(totalInboundCount, vendorBreakdown);
        } catch (IOException e) {
            log.error("[Report] 입고 섹션 집계 실패 storeId={}", storeId, e);
            throw new AnalyticsException(AnalyticsErrorCode.REPORT_GENERATION_FAILED);
        }
    }

    // ──────────────────────────────────────────────────────
    // Private Parse Methods
    // ──────────────────────────────────────────────────────

    private List<WasteSection.ReasonEntry> parseReasonBreakdown(SearchResponse<WasteRecordDocument> response, long totalWasteCount) {
        Aggregate byReason = response.aggregations().get(ReportConstants.AGG_BY_REASON);
        if (byReason == null) return Collections.emptyList();

        List<WasteSection.ReasonEntry> result = new ArrayList<>();
        for (StringTermsBucket bucket : byReason.sterms().buckets().array()) {
            double amount = Optional.ofNullable(
                            bucket.aggregations().get(ReportConstants.AGG_SUM_WASTE_AMOUNT))
                    .map(agg -> agg.sum().value())
                    .orElse(0.0);
            double ratio = totalWasteCount > 0
                    ? (bucket.docCount() * 100.0) / totalWasteCount : 0.0;

            result.add(new WasteSection.ReasonEntry(
                    bucket.key().stringValue(),
                    bucket.docCount(),
                    BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP),
                    Math.round(ratio * 10.0) / 10.0
            ));
        }
        return result;
    }

    private List<WasteSection.IngredientEntry> parseTop5Ingredients(SearchResponse<WasteRecordDocument> response) {
        Aggregate byIngredient = response.aggregations().get(ReportConstants.AGG_BY_INGREDIENT);
        if (byIngredient == null) return Collections.emptyList();

        List<WasteSection.IngredientEntry> result = new ArrayList<>();
        for (LongTermsBucket bucket : byIngredient.lterms().buckets().array()) {
            double amount = Optional.ofNullable(
                            bucket.aggregations().get(ReportConstants.AGG_SUM_WASTE_AMOUNT))
                    .map(agg -> agg.sum().value())
                    .orElse(0.0);
            double qty = Optional.ofNullable(
                            bucket.aggregations().get(ReportConstants.AGG_SUM_WASTE_QUANTITY))
                    .map(agg -> agg.sum().value())
                    .orElse(0.0);

            List<Hit<JsonData>> hits = Optional.ofNullable(
                            bucket.aggregations().get(ReportConstants.AGG_TOP_HIT))
                    .map(agg -> agg.topHits().hits().hits())
                    .orElse(Collections.emptyList());

            String name = hits.isEmpty() ? ReportConstants.DEFAULT_UNKNOWN_INGREDIENT
                    : hits.get(0).source().to(WasteRecordDocument.class).productDisplayName();

            result.add(new WasteSection.IngredientEntry(
                    name,
                    BigDecimal.valueOf(qty).setScale(3, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)
            ));
        }
        return result;
    }

    private List<StockInboundSection.VendorEntry> parseVendorBreakdown(SearchResponse<StockInboundDocument> response) {
        Aggregate byVendor = response.aggregations().get(ReportConstants.AGG_BY_VENDOR);
        if (byVendor == null) return Collections.emptyList();

        List<StockInboundSection.VendorEntry> result = new ArrayList<>();
        for (StringTermsBucket bucket : byVendor.sterms().buckets().array()) {
            result.add(new StockInboundSection.VendorEntry(
                    bucket.key().stringValue(),
                    bucket.docCount()
            ));
        }
        return result;
    }
}
