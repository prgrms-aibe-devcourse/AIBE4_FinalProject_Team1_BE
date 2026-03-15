package kr.inventory.domain.analytics.repository.impl;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import kr.inventory.domain.analytics.controller.dto.request.ESStockLogSearchRequest;
import kr.inventory.domain.analytics.document.stock.StockLogDocument;
import kr.inventory.domain.analytics.exception.AnalyticsErrorCode;
import kr.inventory.domain.analytics.exception.AnalyticsException;
import kr.inventory.domain.analytics.repository.StockLogSearchRepositoryCustom;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StockLogSearchRepositoryImpl implements StockLogSearchRepositoryCustom {

	private final ElasticsearchClient elasticsearchClient;

	@Override
	public Page<StockLogDocument> searchStockLogs(Long storeId, ESStockLogSearchRequest request) {
		try {
			SearchResponse<StockLogDocument> response = elasticsearchClient.search(s -> s
				.index("stock_logs") // ElasticsearchIndex.STOCK_LOGS 상숫값 사용 권장
				.from(request.page() * request.size())
				.size(request.size())
				.sort(sort -> sort.field(f -> f.field("createdAt").order(SortOrder.Desc))) // 최신순 정렬
				.query(q -> q.bool(b -> {
					b.filter(f -> f.term(t -> t.field("storeId").value(storeId)));

					if (request.transactionType() != null && !request.transactionType().isBlank()) {
						b.filter(f -> f.term(t -> t.field("transactionType").value(request.transactionType())));
					}

					if (request.referenceType() != null && !request.referenceType().isBlank()) {
						b.filter(f -> f.term(t -> t.field("referenceType").value(request.referenceType())));
					}

					if (request.keyword() != null && !request.keyword().isBlank()) {
						b.must(m -> m.multiMatch(mm -> mm
							.query(request.keyword())
							.fields(List.of("ingredientName^2", "productDisplayName"))
						));
					}
					
					if (request.startDate() != null || request.endDate() != null) {
						b.filter(f -> f.range(r -> r
							.date(d -> {
								d.field("createdAt");
								if (request.startDate() != null)
									d.gte(request.startDate().toString());
								if (request.endDate() != null)
									d.lte(request.endDate().toString());
								return d;
							})
						));
					}
					return b;
				})), StockLogDocument.class);

			List<StockLogDocument> content = response.hits().hits().stream()
				.map(Hit::source)
				.toList();

			long total = response.hits().total() != null ? response.hits().total().value() : 0;

			return new PageImpl<>(content, PageRequest.of(request.page(), request.size()), total);

		} catch (IOException e) {
			throw new AnalyticsException(AnalyticsErrorCode.STOCK_ANALYSIS_FAILED);
		}
	}
}