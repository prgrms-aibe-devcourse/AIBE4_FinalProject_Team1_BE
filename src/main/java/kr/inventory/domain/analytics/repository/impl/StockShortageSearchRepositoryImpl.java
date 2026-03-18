package kr.inventory.domain.analytics.repository.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import kr.inventory.domain.analytics.constant.ElasticsearchIndex;
import kr.inventory.domain.analytics.controller.dto.request.ESStockShortageSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.StockShortageSummaryResponse;
import kr.inventory.domain.analytics.document.stock.StockShortageDocument;
import kr.inventory.domain.analytics.repository.StockShortageSearchRepositoryCustom;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class StockShortageSearchRepositoryImpl implements StockShortageSearchRepositoryCustom {

	private final ElasticsearchClient elasticsearchClient;

	@Override
	public List<StockShortageSummaryResponse> getShortageSummary(Long storeId, ESStockShortageSearchRequest request) {
		try {
			SearchResponse<StockShortageDocument> response = elasticsearchClient.search(s -> s
				.index(ElasticsearchIndex.StockShortage)
				.size(0) // 집계 중심이므로 검색 결과(Hits)는 제외
				.query(q -> q.bool(b -> {
					// 1. 필수 필터: 매장 ID
					b.filter(f -> f.term(t -> t.field("storeId").value(storeId)));

					// 2. 동적 상태(status) 필터 추가
					if (request.status() != null && !request.status().isBlank()) {
						String upperStatus = request.status().toUpperCase();
						b.filter(f -> f.term(t -> t.field("status").value(upperStatus)));
					}

					// 3. 동적 키워드 검색 (식재료명)
					if (request.keyword() != null && !request.keyword().isBlank()) {
						b.must(m -> m.multiMatch(mm -> mm
							.query(request.keyword())
							.fields(List.of("ingredientName^3", "ingredientName.ko"))
						));
					}

					if (request.from() != null || request.to() != null) {
						b.filter(f -> f.range(r -> r
							.date(d -> {
								d.field("createdAt");
								if (request.from() != null)
									d.gte(request.from().toString());
								if (request.to() != null)
									d.lte(request.to().toString());
								return d;
							})
						));
					}
					return b;
				}))
				.aggregations("by_ingredient", a -> a
					.terms(t -> t.field("ingredientId").size(100))
					.aggregations("total_shortage", sa -> sa.sum(sm -> sm.field("shortageAmount")))
					.aggregations("related_orders", sa -> sa.terms(t -> t.field("salesOrderId").size(5)))
					.aggregations("latest_time", sa -> sa.max(m -> m.field("createdAt")))
					.aggregations("top_hit", sa -> sa.topHits(th -> th.size(1)))
				), StockShortageDocument.class);

			return parseShortageBuckets(response);
		} catch (IOException e) {
			throw new RuntimeException("Elasticsearch 집계 쿼리 실행 실패", e);
		}
	}

	private List<StockShortageSummaryResponse> parseShortageBuckets(SearchResponse<StockShortageDocument> response) {
		Aggregate aggregate = response.aggregations().get("by_ingredient");
		if (aggregate == null || !aggregate.isLterms()) {
			return List.of();
		}

		return aggregate.lterms().buckets().array().stream()
			.map(bucket -> {
				double totalShortage = bucket.aggregations().get("total_shortage").sum().value();
				double lastTimeEpoch = bucket.aggregations().get("latest_time").max().value();

				// 부족 수량이 없으면 결과에서 제외
				if (totalShortage <= 0) {
					return null;
				}

				// 관련 주문 ID 리스트 추출
				List<Long> orderIds = bucket.aggregations().get("related_orders").lterms().buckets().array().stream()
					.map(b -> Long.valueOf(b.key()))
					.toList();

				var hits = bucket.aggregations().get("top_hit").topHits().hits().hits();
				String name = "알 수 없는 식재료";
				String status = "UNKNOWN";

				if (!hits.isEmpty()) {
					StockShortageDocument doc = hits.get(0).source().to(StockShortageDocument.class);
					name = doc.ingredientName();
					status = doc.status();
				}

				return new StockShortageSummaryResponse(
					Long.valueOf(bucket.key()),
					name,
					BigDecimal.valueOf(totalShortage),
					status, // 응답 DTO에 status 필드 반영
					bucket.docCount(),
					convertEpochToOffsetDateTime(lastTimeEpoch),
					orderIds
				);
			})
			.filter(Objects::nonNull)
			.toList();
	}

	private OffsetDateTime convertEpochToOffsetDateTime(double epoch) {
		if (Double.isInfinite(epoch) || Double.isNaN(epoch) || epoch <= 0) {
			return null;
		}
		return Instant.ofEpochMilli((long)epoch)
			.atZone(ZoneId.systemDefault())
			.toOffsetDateTime();
	}
}