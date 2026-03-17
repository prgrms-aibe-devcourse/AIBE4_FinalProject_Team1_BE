package kr.inventory.domain.analytics.service.indexing;

import kr.inventory.domain.analytics.document.stock.StockLogDocument;
import kr.inventory.domain.analytics.repository.StockLogSearchRepository;
import kr.inventory.domain.stock.entity.StockLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockLogIndexingService {

    private final StockLogSearchRepository stockLogSearchRepository;

    public void index(StockLog stockLog) {
        StockLogDocument doc = StockLogDocument.from(stockLog);
        stockLogSearchRepository.save(doc);
        log.debug("[ES] 재고 로그 인덱싱 완료 logId={}", stockLog.getLogId());
    }
}
