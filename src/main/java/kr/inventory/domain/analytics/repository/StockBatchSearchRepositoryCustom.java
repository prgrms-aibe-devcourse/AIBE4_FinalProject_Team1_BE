package kr.inventory.domain.analytics.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import kr.inventory.domain.analytics.controller.dto.response.StockAnalyticResponse;
import kr.inventory.domain.analytics.document.stock.IngredientStockBatchDocument;

public interface StockBatchSearchRepositoryCustom {
	List<IngredientStockBatchDocument> searchStockBatches(
		Long storeId, String keyword, String status, Integer daysUntilExpiry
	);

	Map<Long, StockAnalyticResponse.StockPart> aggregateStockMap(Long storeId);

	Map<Long, StockAnalyticResponse.WastePart> aggregateWasteMap(Long storeId);
}
