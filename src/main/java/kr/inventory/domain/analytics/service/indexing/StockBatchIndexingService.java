package kr.inventory.domain.analytics.service.indexing;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import kr.inventory.domain.analytics.document.stock.IngredientStockBatchDocument;
import kr.inventory.domain.analytics.repository.StockBatchSearchRepository;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockBatchIndexingService {
	private final StockBatchSearchRepository stockBatchSearchRepository;

	@Async
	public void index(IngredientStockBatch batch) {
		try {
			IngredientStockBatchDocument doc = IngredientStockBatchDocument.from(batch,
				batch.getIngredient().getLowStockThreshold());

			stockBatchSearchRepository.save(doc);
			log.debug("[ES] 재고 배치 인덱싱 완료 batchId={}", batch.getBatchId());
		} catch (Exception e) {
			log.error("[ES] 재고 배치 인덱싱 실패 batchId={}", batch.getBatchId(), e);
		}

	}
}