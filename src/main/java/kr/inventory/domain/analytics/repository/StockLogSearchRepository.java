package kr.inventory.domain.analytics.repository;

import kr.inventory.domain.analytics.document.stock.StockLogDocument;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface StockLogSearchRepository
	extends ElasticsearchRepository<StockLogDocument, String>, StockLogSearchRepositoryCustom {
}
