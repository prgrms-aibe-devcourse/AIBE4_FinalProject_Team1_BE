package kr.inventory.domain.analytics.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import co.elastic.clients.json.JsonData;
import kr.inventory.domain.analytics.document.stock.IngredientStockBatchDocument;
import kr.inventory.domain.store.service.StoreAccessValidator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockAnalyticService {

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
}