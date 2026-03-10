package kr.inventory.domain.analytics.repository;

import kr.inventory.domain.analytics.document.stock.WasteRecordDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface WasteRecordSearchRepository extends ElasticsearchRepository<WasteRecordDocument, String> {
}
