package kr.inventory.domain.stock.service;

import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.exception.SalesErrorCode;
import kr.inventory.domain.sales.exception.SalesException;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.stock.controller.dto.StockRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockManagerFacade {
    private final TheoreticalUsageService theoreticalUsageService;
    private final SalesOrderRepository salesOrderRepository;
    private final StockService stockService;

    @Transactional
    public void processOrderStockDeduction(StockRequestDto.OrderDeductionRequest request) {
        SalesOrder salesOrder = salesOrderRepository.findById(request.salesOrderId())
                .orElseThrow(() -> new SalesException(SalesErrorCode.SALES_ORDER_NOT_FOUND));

        if (salesOrder.isStockProcessed()) {
            log.info("이미 재고가 차감된 주문입니다. 주문 ID: {}", request.salesOrderId());
            return;
        }

        Map<Long, BigDecimal> usageMap = theoreticalUsageService.calculateOrderUsage(salesOrder);

        Map<Long, BigDecimal> shortageMap = stockService.deductStockWithFEFO(usageMap);

        if(!shortageMap.isEmpty()){
            handleStockShortage(salesOrder.getSalesOrderId(), shortageMap);
        }

        salesOrder.markAsStockProcessed();
    }

    private void handleStockShortage(Long orderId, Map<Long, BigDecimal> shortageMap) {
        shortageMap.forEach((ingredientId, amount) -> {
            log.warn("재고 부족 발생 - 주문: {}, 식재료 ID: {}, 부족량: {}", orderId, ingredientId, amount);

            // TODO: [비즈니스 요구사항]
            // 1. Shortage 테이블에 기록하여 추후 발주 데이터로 활용 필요
            // 2. 관리자에게 '재고 부족' 알림 발송 로직 추가 필요
        });
    }
}
