package kr.inventory.domain.analytics.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import kr.inventory.domain.analytics.document.stock.IngredientStockBatchDocument;

public interface StockBatchSearchRepository extends ElasticsearchRepository<IngredientStockBatchDocument, String> {
}
