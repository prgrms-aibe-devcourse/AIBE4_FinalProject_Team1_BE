package kr.inventory.domain.analytics.repository;

import kr.inventory.domain.analytics.document.sales.SalesOrderDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SalesOrderSearchRepository extends ElasticsearchRepository<SalesOrderDocument, String> {
}
