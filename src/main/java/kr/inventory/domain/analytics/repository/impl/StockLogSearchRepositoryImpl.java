package kr.inventory.domain.analytics.repository.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import kr.inventory.domain.analytics.controller.dto.request.ESStockLogSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.StockLogAnalyticResponse;
import kr.inventory.domain.analytics.document.stock.StockLogDocument;
import kr.inventory.domain.analytics.repository.StockLogSearchRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockLogSearchRepositoryImpl implements StockLogSearchRepositoryCustom {

	private final ElasticsearchClient elasticsearchClient;

	@Override
	public List<StockLogAnalyticResponse> searchStockLogs(Long storeId, ESStockLogSearchRequest request) {
		try {
			SearchResponse<StockLogDocument> response = elasticsearchClient.search(s -> s
				.index("stock_logs")
				.from(0)
                    .size(request.resolvedLimit())
				.sort(sort -> sort.field(f -> f.field("createdAt").order(SortOrder.Desc)))
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
						b.filter(f -> f.range(r -> r.date(d -> {
							d.field("createdAt");
							if (request.startDate() != null)
								d.gte(request.startDate().toString());
							if (request.endDate() != null)
								d.lte(request.endDate().toString());
							return d;
						})));
					}
					return b;
				})), StockLogDocument.class);

			return response.hits().hits().stream()
				.map(hit -> {
					StockLogDocument doc = hit.source();
					return doc != null ? StockLogAnalyticResponse.from(doc) : null;
				})
				.filter(java.util.Objects::nonNull) // 혹시 모를 null 방어
				.toList();

		} catch (IOException e) {
			throw new RuntimeException("Stock log search failed", e);
		}
	}
}