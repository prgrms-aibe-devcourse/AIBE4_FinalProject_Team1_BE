package kr.inventory.domain.analytics.repository.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.LongTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import kr.inventory.domain.analytics.controller.dto.response.StockShortageSummaryResponse;
import kr.inventory.domain.analytics.document.stock.StockShortageDocument;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.repository.StockShortageSearchRepositoryCustom;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StockShortageSearchRepositoryImpl implements StockShortageSearchRepositoryCustom {
	private final ElasticsearchClient elasticsearchClient;
	private final ElasticsearchOperations elasticsearchOperations;

	@Override
	public List<StockShortageSummaryResponse> getShortageSummary(Long storeId) {
		try {
			SearchResponse<StockShortageDocument> response = elasticsearchClient.search(s -> s
				.index("stock_shortage")
				.size(0) // 검색 결과 자체는 필요 없고 집계 결과만 필요
				.query(q -> q.bool(b -> b
					.filter(f -> f.term(t -> t.field("storeId").value(storeId)))
				))
				.aggregations("by_ingredient", a -> a
					.terms(t -> t.field("ingredientId").size(100)) // 식재료별 그룹화
					.aggregations("total_shortage", sa -> sa.sum(sm -> sm.field("shortageAmount")))
					.aggregations("related_orders", sa -> sa.terms(t -> t.field("salesOrderId").size(5))) // 관련 주문 ID 5개
					.aggregations("latest_time", sa -> sa.max(m -> m.field("createdAt")))
					.aggregations("top_hit", sa -> sa.topHits(th -> th.size(1))) // 이름 추출용
				), StockShortageDocument.class);

			return parseShortageBuckets(response);
		} catch (IOException e) {
			throw new AnalyticsException(AnalyticsErrorCode.STOCK_ANALYSIS_FAILED);
		}
	}

	private List<StockShortageSummaryResponse> parseShortageBuckets(SearchResponse<StockShortageDocument> response) {
		Aggregate aggregate = response.aggregations().get("by_ingredient");
		if (aggregate == null)
			return Collections.emptyList();

		return aggregate.lterms().buckets().array().stream()
			.map(bucket -> {
				double totalShortage = bucket.aggregations().get("total_shortage").sum().value();
				double lastTime = bucket.aggregations().get("latest_time").max().value();

				// 관련 주문 ID 리스트 추출
				List<Long> orderIds = bucket.aggregations().get("related_orders").lterms().buckets().array().stream()
					.map(LongTermsBucket::key)
					.toList();

				// 이름 추출 (top_hits 활용)
				var hits = bucket.aggregations().get("top_hit").topHits().hits().hits();
				String name = hits.isEmpty() ? "알 수 없는 식재료" :
					hits.get(0).source().to(StockShortageDocument.class).ingredientName();

				return new StockShortageSummaryResponse(
					bucket.key(),
					name,
					BigDecimal.valueOf(totalShortage),
					bucket.docCount(), // 해당 버킷에 담긴 문서 수 = 주문 수
					convertEpochToOffsetDateTime(lastTime),
					orderIds
				);
			})
			.toList();
	}

	private OffsetDateTime convertEpochToOffsetDateTime(double epoch) {
		if (Double.isInfinite(epoch) || Double.isNaN(epoch) || epoch <= 0) {
			return null;
		}

		// 1. Instant 생성 (밀리초 단위)
		Instant instant = Instant.ofEpochMilli((long)epoch);

		// 2. 시스템 기본 시간대를 사용하여 OffsetDateTime으로 변환
		return instant.atZone(ZoneId.systemDefault()).toOffsetDateTime();
	}
}
