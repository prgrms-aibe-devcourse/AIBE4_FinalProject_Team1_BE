package kr.inventory.domain.analytics.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import kr.inventory.domain.analytics.document.stock.StockShortageDocument;

public interface StockShortageSearchRepository
	extends ElasticsearchRepository<StockShortageDocument, String>, StockShortageSearchRepositoryCustom {
}
