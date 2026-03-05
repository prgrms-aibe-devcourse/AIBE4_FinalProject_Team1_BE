package kr.inventory.domain.stock.service;

import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.stock.repository.StockShortageRepository;
import kr.inventory.domain.stock.service.command.StockDeductionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockManagerFacade {
	private final TheoreticalUsageService theoreticalUsageService;
	private final SalesOrderRepository salesOrderRepository;
	private final StockService stockService;
    private final StockShortageRepository stockShortageRepository;
    private final StockShortageService stockShortageService;

	@Transactional
	public void processOrderStockDeduction(SalesOrder salesOrder, List<SalesOrderItem> items) {
        Long storeId = salesOrder.getStore().getStoreId();
        Long salesOrderId = salesOrder.getSalesOrderId();

		if (salesOrder.isStockProcessed()) {
			log.info("이미 재고가 차감된 주문입니다. 주문 ID: {}", salesOrderId);
			return;
		}

		Map<Long, BigDecimal> usageMap = theoreticalUsageService.calculateOrderUsage(storeId, items);

        StockDeductionRequest request = StockDeductionRequest.of(storeId, salesOrderId, usageMap);
		Map<Long, BigDecimal> shortageMap = stockService.deductStockWithFEFO(request);

		if (!shortageMap.isEmpty()) {
            log.warn("재고 부족 발생 - 주문: {}, 부족 품목 수: {}", salesOrderId, shortageMap.size());
            stockShortageService.recordShortages(storeId, salesOrderId, usageMap, shortageMap);
		}

		salesOrder.markAsStockProcessed();
	}
}
