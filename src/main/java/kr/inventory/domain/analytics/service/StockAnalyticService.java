package kr.inventory.domain.analytics.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.analytics.controller.dto.response.StockAnalyticResponse;
import kr.inventory.domain.analytics.document.stock.IngredientStockBatchDocument;
import kr.inventory.domain.analytics.document.stock.WasteRecordDocument;
import kr.inventory.domain.analytics.exception.ESException;
import kr.inventory.domain.analytics.exception.EsErrorCode;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAnalyticService {

	private final ElasticsearchClient elasticsearchClient;
	private final ElasticsearchOperations elasticsearchOperations;
	private final StoreAccessValidator storeAccessValidator;

	public List<IngredientStockBatchDocument> searchStockBatches(
		Long userId,
		UUID storePublicId,
		String keyword,
		String status,
		Boolean isLowStock,
		Integer daysUntilExpiry // 유통기한 임박(N일 이내) 필터 추가
	) {

		// 1. 보안 검증 및 내부 ID 획득
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		// 2. NativeQuery 빌더 구성
		NativeQuery query = NativeQuery.builder()
			.withQuery(q -> q.bool(b -> {
					// [기본] 매장 식별 및 상태 필터
					b.filter(f -> f.term(t -> t.field("storeId").value(storeId)));
					b.filter(f -> f.term(t -> t.field("status").value(status != null ? status : "OPEN")));

					// [시나리오 2, 3] 통합 검색 (상품명, 제조사, 카테고리 포함)
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

						b.filter(f -> f.range(r -> r
							.untyped(u -> u
								.field("expirationDate")
								.gte(JsonData.of(today))
								.lte(JsonData.of(targetDate))
							)
						));
					}
					// TODO: 품절 주의 필터

					return b;
				})
			)
			.withSort(Sort.by(Sort.Order.asc("expirationDate"))) // 기본은 유통기한 순
			.build();

		return elasticsearchOperations.search(query, IngredientStockBatchDocument.class)
			.stream()
			.map(SearchHit::getContent)
			.toList();
	}

	public List<StockAnalyticResponse> getIntegratedAnalysis(Long userId, UUID storePublicId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		try {
			Map<Long, StockAnalyticResponse.StockPart> stockMap = fetchStockMap(storeId);

			Map<Long, StockAnalyticResponse.WastePart> wasteMap = fetchWasteMap(storeId);

			Set<Long> allIds = new HashSet<>(stockMap.keySet());
			allIds.addAll(wasteMap.keySet());

			return allIds.stream()
				.map(id -> StockAnalyticResponse.of(
					id,
					stockMap.getOrDefault(id, StockAnalyticResponse.StockPart.empty()),
					wasteMap.getOrDefault(id, StockAnalyticResponse.WastePart.empty())
				))
				.toList();

		} catch (IOException e) {
			log.error("[Elasticsearch Error] storeId: {} 조회 중 통신 실패", storeId, e);
			throw new ESException(EsErrorCode.STOCK_ANALYSIS_FAILED);
		}
	}

	private Map<Long, StockAnalyticResponse.StockPart> fetchStockMap(Long storeId) throws IOException {
		SearchResponse<IngredientStockBatchDocument> response = elasticsearchClient.search(s -> s
				.index(ElasticsearchIndex.STOCK_BATCH)
				.size(0)
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
				),
			IngredientStockBatchDocument.class
		);

		return parseStockBuckets(response);
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
					String name = hits.isEmpty() ? "알 수 없는 식재료" :
						hits.get(0).source().to(IngredientStockBatchDocument.class).productDisplayName();

					log.info("ES Source: " + hits.get(0).source().toString());

					return new StockAnalyticResponse.StockPart(
						name,
						BigDecimal.valueOf(qty),
						convertEpochToDate(minExp),
						qty < threshold,
						bucket.docCount()
					);
				}
			));
	}

	private Map<Long, StockAnalyticResponse.WastePart> fetchWasteMap(Long storeId) throws IOException {
		SearchResponse<WasteRecordDocument> response = elasticsearchClient.search(s -> s
				.index(ElasticsearchIndex.WASTE_RECORDS)
				.size(0)
				.query(q -> q.bool(b -> b.filter(f -> f.term(t -> t.field("storeId").value(storeId)))))
				.aggregations("by_ingredient", a -> a
					.terms(t -> t.field("ingredientId").size(1000))
					.aggregations("sum_qty", sa -> sa.sum(sm -> sm.field("wasteQuantity")))
					.aggregations("sum_amount", sa -> sa.sum(sm -> sm.field("wasteAmount")))
					.aggregations("top_hit", sa -> sa.topHits(th -> th.size(1)))
				),
			WasteRecordDocument.class
		);

		return parseWasteBuckets(response);
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
					String name = hits.isEmpty() ? "알 수 없는 식재료" :
						hits.get(0).source().to(WasteRecordDocument.class).productDisplayName();

					return new StockAnalyticResponse.WastePart(
						name,
						BigDecimal.valueOf(qty),
						BigDecimal.valueOf(amount),
						bucket.docCount()
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
