package kr.inventory.domain.analytics.repository.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
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
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StockShortageSearchRepositoryImpl implements StockShortageSearchRepositoryCustom {

	private final ElasticsearchClient elasticsearchClient;

	@Override
	public List<StockShortageSummaryResponse> getShortageSummary(Long storeId, ESStockShortageSearchRequest request) {
		try {
			SearchResponse<StockShortageDocument> response = elasticsearchClient.search(s -> s
				.index(ElasticsearchIndex.StockShortage)
				.size(0)
				.query(q -> q.bool(b -> {
					b.filter(f -> f.term(t -> t.field("storeId").value(storeId)));

					if (request.status() != null && !request.status().isBlank()) {
						String upperStatus = request.status().toUpperCase();
						b.filter(f -> f.term(t -> t.field("status").value(upperStatus)));
					}

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
					// affectedOrderCount 계산을 위한 cardinality 집계 (주문 건수 중복 제거)
					.aggregations("affected_orders", sa -> sa.cardinality(c -> c.field("salesOrderId")))
					.aggregations("latest_time", sa -> sa.max(m -> m.field("createdAt")))
					.aggregations("top_hit", sa -> sa.topHits(th -> th.size(1)))
				), StockShortageDocument.class);

			return parseShortageBuckets(response);
		} catch (IOException e) {
			throw new RuntimeException("Elasticsearch 집계 쿼리 실행 실패", e);
		}
	}

	private List<StockShortageSummaryResponse> parseShortageBuckets(SearchResponse<StockShortageDocument> response) {
		if (response.aggregations().get("by_ingredient") == null) {
			return List.of();
		}

		var aggregate = response.aggregations().get("by_ingredient");

		// ingredientId는 보통 lterms(Long)이므로 lterms() 사용
		return aggregate.lterms().buckets().array().stream()
			.map(bucket -> {
				double totalShortage = bucket.aggregations().get("total_shortage").sum().value();
				double lastTimeEpoch = bucket.aggregations().get("latest_time").max().value();
				// cardinality 집계 결과로 주문 건수 산출
				long affectedOrderCount = (long)bucket.aggregations().get("affected_orders").cardinality().value();

				if (totalShortage <= 0) {
					return null;
				}

				// top_hit에서 세부 정보(PublicId, Name, Status) 추출
				var hits = bucket.aggregations().get("top_hit").topHits().hits().hits();
				UUID publicId = null;
				String name = "알 수 없는 식재료";
				String status = "UNKNOWN";

				if (!hits.isEmpty()) {
					StockShortageDocument doc = hits.get(0).source().to(StockShortageDocument.class);
					publicId = doc.stockShortagePublicId();
					name = doc.ingredientName();
					status = doc.status();
				}

				return StockShortageSummaryResponse.of(
					publicId,
					name,
					BigDecimal.valueOf(totalShortage),
					status,
					affectedOrderCount, // affectedOrderCount 매핑
					convertEpochToOffsetDateTime(lastTimeEpoch)
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