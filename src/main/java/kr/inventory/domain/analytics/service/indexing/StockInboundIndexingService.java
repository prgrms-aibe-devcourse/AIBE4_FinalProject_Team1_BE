package kr.inventory.domain.analytics.service.indexing;

import kr.inventory.domain.analytics.document.stock.StockInboundDocument;
import kr.inventory.domain.analytics.repository.StockInboundSearchRepository;
import kr.inventory.domain.stock.entity.StockInbound;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInboundIndexingService {

    private final StockInboundSearchRepository stockInboundSearchRepository;

    public void index(StockInbound inbound) {
        StockInboundDocument doc = StockInboundDocument.from(inbound);
        stockInboundSearchRepository.save(doc);
        log.debug("[ES] 입고 인덱싱 완료 inboundId={}", inbound.getInboundId());
    }
}
