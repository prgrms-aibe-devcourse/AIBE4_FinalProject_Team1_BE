package kr.inventory.domain.analytics.service;

import kr.inventory.domain.analytics.document.sales.SalesOrderDocument;
import kr.inventory.domain.analytics.repository.SalesOrderSearchRepository;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesIndexingService {

    private final SalesOrderSearchRepository salesOrderSearchRepository;

    public void index(SalesOrder order, List<SalesOrderItem> items) {
        SalesOrderDocument doc = SalesOrderDocument.from(order, items);
        salesOrderSearchRepository.save(doc);
        log.debug("[ES] 매출 주문 인덱싱 완료 salesOrderId={}", order.getSalesOrderId());
    }
}
