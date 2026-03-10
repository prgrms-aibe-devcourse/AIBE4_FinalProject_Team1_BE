package kr.inventory.domain.analytics.repository;

import kr.inventory.domain.analytics.document.stock.StockInboundDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface StockInboundSearchRepository extends ElasticsearchRepository<StockInboundDocument, String> {
}
