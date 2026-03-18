package kr.inventory.domain.analytics.repository.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.analytics.controller.dto.response.StockAnalyticResponse;
import kr.inventory.domain.analytics.document.stock.IngredientStockBatchDocument;
import kr.inventory.domain.analytics.document.stock.WasteRecordDocument;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.repository.StockBatchSearchRepositoryCustom;
import kr.inventory.domain.reference.entity.enums.IngredientUnit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class StockBatchSearchRepositoryImpl implements StockBatchSearchRepositoryCustom {

	private final ElasticsearchClient elasticsearchClient;
	private final ElasticsearchOperations elasticsearchOperations;

	@Override
	public List<IngredientStockBatchDocument> searchStockBatches(
		Long storeId, String keyword, String status, Integer daysUntilExpiry
	) {
		NativeQuery query = NativeQuery.builder()
			.withQuery(q -> q.bool(b -> {
				b.filter(f -> f.term(t -> t.field("storeId").value(storeId)));
				b.filter(f -> f.term(t -> t.field("status").value(status != null ? status : "OPEN")));

				if (keyword != null && !keyword.isBlank()) {
					b.must(m -> m.multiMatch(mm -> mm
						.query(keyword)
						.fields(List.of("productDisplayName^3", "vendorName"))
						.fuzziness("AUTO")
					));
				}

				if (daysUntilExpiry != null) {
					String today = LocalDate.now().toString();
					String targetDate = LocalDate.now().plusDays(daysUntilExpiry).toString();
					b.filter(f -> f.range(r -> r.untyped(
						u -> u.field("expirationDate").gte(JsonData.of(today)).lte(JsonData.of(targetDate)))));
				}
				return b;
			}))
			.withSort(Sort.by(Sort.Order.asc("expirationDate")))
			.build();

		return elasticsearchOperations.search(query, IngredientStockBatchDocument.class)
			.stream().map(SearchHit::getContent).toList();
	}

	@Override
	public Map<Long, StockAnalyticResponse.StockPart> aggregateStockMap(Long storeId) {
		try {
			SearchResponse<IngredientStockBatchDocument> response = elasticsearchClient.search(s -> s
				.index(ElasticsearchIndex.STOCK_BATCH).size(0)
				.query(q -> q.bool(b -> b
					.filter(f -> f.term(t -> t.field("storeId").value(v -> v.longValue(storeId))))
					.filter(f -> f.term(t -> t.field("status").value(v -> v.stringValue("OPEN"))))
				))
				.aggregations("by_ingredient", a -> a
					.terms(t -> t.field("ingredientId").size(1000))
					.aggregations("total_qty", sa -> sa.sum(sm -> sm.field("remainingQuantity")))
					.aggregations("min_expiry", sa -> sa.min(mn -> mn.field("expirationDate")))
					.aggregations("avg_threshold", sa -> sa.avg(av -> av.field("lowStockThreshold")))
					.aggregations("top_hit", sa -> sa.topHits(th -> th.size(1)))
				), IngredientStockBatchDocument.class);

			return parseStockBuckets(response);
		} catch (IOException e) {
			throw new AnalyticsException(AnalyticsErrorCode.STOCK_ANALYSIS_FAILED);
		}
	}

	@Override
	public Map<Long, StockAnalyticResponse.WastePart> aggregateWasteMap(Long storeId) {
		try {
			SearchResponse<WasteRecordDocument> response = elasticsearchClient.search(s -> s
				.index(ElasticsearchIndex.WASTE_RECORDS).size(0)
				.query(q -> q.bool(b -> b.filter(f -> f.term(t -> t.field("storeId").value(storeId)))))
				.aggregations("by_ingredient", a -> a
					.terms(t -> t.field("ingredientId").size(1000))
					.aggregations("sum_qty", sa -> sa.sum(sm -> sm.field("wasteQuantity")))
					.aggregations("sum_amount", sa -> sa.sum(sm -> sm.field("wasteAmount")))
					.aggregations("top_hit", sa -> sa.topHits(th -> th.size(1)))
				), WasteRecordDocument.class);

			return parseWasteBuckets(response);
		} catch (IOException e) {
			throw new AnalyticsException(AnalyticsErrorCode.STOCK_ANALYSIS_FAILED);
		}
	}

	private Map<Long, StockAnalyticResponse.StockPart> parseStockBuckets(
		SearchResponse<IngredientStockBatchDocument> response) {
		Aggregate aggregate = response.aggregations().get("by_ingredient");
		if (aggregate == null)
			return Collections.emptyMap();

		return aggregate.lterms().buckets().array().stream()
			.collect(Collectors.toMap(
				LongTermsBucket::key,
				bucket -> {
					double qty = bucket.aggregations().get("total_qty").sum().value();
					double minExp = bucket.aggregations().get("min_expiry").min().value();
					double threshold = bucket.aggregations().get("avg_threshold").avg().value();

					var hits = bucket.aggregations().get("top_hit").topHits().hits().hits();

					// 1. 도큐먼트 추출 (안전한 null 처리를 포함)
					IngredientStockBatchDocument doc = hits.isEmpty() ? null :
						hits.get(0).source().to(IngredientStockBatchDocument.class);

					String name = (doc != null) ? doc.productDisplayName() : "알 수 없는 식재료";
					// 2. 추가한 unit 필드 활용 (문자열 -> Enum 변환)
					IngredientUnit unit = (doc != null && doc.unit() != null) ?
						IngredientUnit.valueOf(doc.unit()) : IngredientUnit.EA;

					return new StockAnalyticResponse.StockPart(
						name,
						BigDecimal.valueOf(qty),
						convertEpochToDate(minExp),
						qty < threshold,
						bucket.docCount(),
						unit
					);
				}
			));
	}

	private Map<Long, StockAnalyticResponse.WastePart> parseWasteBuckets(SearchResponse<WasteRecordDocument> response) {
		Aggregate aggregate = response.aggregations().get("by_ingredient");
		if (aggregate == null)
			return Collections.emptyMap();

		return aggregate.lterms().buckets().array().stream()
			.collect(Collectors.toMap(
				LongTermsBucket::key,
				bucket -> {
					double qty = bucket.aggregations().get("sum_qty").sum().value();
					double amount = bucket.aggregations().get("sum_amount").sum().value();
					var hits = bucket.aggregations().get("top_hit").topHits().hits().hits();

					WasteRecordDocument doc = hits.isEmpty() ? null :
						hits.get(0).source().to(WasteRecordDocument.class);

					String name = (doc != null) ? doc.productDisplayName() : "알 수 없는 식재료";

					IngredientUnit unit = (doc != null && doc.unit() != null) ?
						IngredientUnit.valueOf(doc.unit()) : IngredientUnit.EA;

					return new StockAnalyticResponse.WastePart(
						name,
						BigDecimal.valueOf(qty),
						BigDecimal.valueOf(amount),
						bucket.docCount(),
						unit
					);
				}
			));
	}

	private LocalDate convertEpochToDate(double epoch) {
		if (Double.isInfinite(epoch) || Double.isNaN(epoch) || epoch <= 0)
			return null;
		return Instant.ofEpochMilli((long)epoch).atZone(ZoneId.systemDefault()).toLocalDate();
	}
}
