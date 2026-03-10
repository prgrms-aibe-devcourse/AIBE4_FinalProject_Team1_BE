package kr.inventory.domain.analytics.service;

import java.util.List;

import org.springframework.stereotype.Service;

import kr.inventory.domain.analytics.document.sales.SalesOrderDocument;
import kr.inventory.domain.analytics.document.stock.IngredientStockBatchDocument;
import kr.inventory.domain.analytics.repository.SalesOrderSearchRepository;
import kr.inventory.domain.analytics.repository.StockBatchSearchRepository;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockBatchIndexingService {
	private final StockBatchSearchRepository stockBatchSearchRepository;

	public void index(IngredientStockBatch batch) {
		IngredientStockBatchDocument doc = IngredientStockBatchDocument.from(batch);
		stockBatchSearchRepository.save(doc);
		log.debug("[ES] 재고 배치 인덱싱 완료 batchId={}", batch.getBatchId());
	}
}