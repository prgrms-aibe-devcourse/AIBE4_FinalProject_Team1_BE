package kr.inventory.domain.analytics.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import kr.inventory.domain.analytics.controller.dto.request.ESStockLogSearchRequest;
import kr.inventory.domain.analytics.controller.dto.response.StockLogAnalyticResponse;
import kr.inventory.domain.analytics.document.stock.StockLogDocument;

public interface StockLogSearchRepositoryCustom {
	List<StockLogAnalyticResponse> searchStockLogs(Long storeId, ESStockLogSearchRequest request);
}
