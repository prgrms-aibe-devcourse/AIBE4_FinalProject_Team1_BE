package kr.inventory.domain.analytics.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import kr.inventory.domain.analytics.controller.dto.request.ESStockLogSearchRequest;
import kr.inventory.domain.analytics.document.stock.StockLogDocument;

public interface StockLogSearchRepositoryCustom {
	Page<StockLogDocument> searchStockLogs(Long storeId, ESStockLogSearchRequest request);
}
