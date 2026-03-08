package kr.inventory.domain.sales.service;

import kr.inventory.domain.sales.controller.dto.request.SalesLedgerSearchRequest;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderDetailResponse;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerOrderSummaryResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.SalesOrderItem;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.exception.SalesOrderErrorCode;
import kr.inventory.domain.sales.exception.SalesOrderException;
import kr.inventory.domain.sales.repository.SalesOrderItemRepository;
import kr.inventory.domain.sales.repository.SalesOrderRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesLedgerService {

    private final StoreAccessValidator storeAccessValidator;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;

    public PageResponse<SalesLedgerOrderSummaryResponse> getSalesLedgerOrders(
            Long userId,
            UUID storePublicId,
            SalesLedgerSearchRequest request,
            Pageable pageable
    ) {
        validateSearchPeriod(request.from(), request.to());

        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        SalesOrderStatus statusFilter = request.status();

        Page<SalesOrder> orderPage = salesOrderRepository.findSalesLedgerOrders(
                storeId,
                request.from(),
                request.to(),
                statusFilter,
                request.type(),
                pageable
        );

        Map<Long, Long> itemCountByOrderId = getItemCountMap(orderPage.getContent());

        Page<SalesLedgerOrderSummaryResponse> responsePage = orderPage.map(order -> {
            Long itemCount = itemCountByOrderId.getOrDefault(order.getSalesOrderId(), 0L);
            BigDecimal refundAmount = calculateRefundAmount(order);
            BigDecimal netAmount = calculateNetAmount(order.getTotalAmount(), refundAmount);
            return SalesLedgerOrderSummaryResponse.from(order, Math.toIntExact(itemCount), refundAmount, netAmount);
        });

        return PageResponse.from(responsePage);
    }

    public SalesLedgerOrderDetailResponse getSalesLedgerOrder(
            Long userId,
            UUID storePublicId,
            UUID orderPublicId
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        SalesOrder order = salesOrderRepository.findByOrderPublicIdWithItems(orderPublicId, storeId)
                .orElseThrow(() -> new SalesOrderException(SalesOrderErrorCode.SALES_ORDER_NOT_FOUND));

        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderSalesOrderId(order.getSalesOrderId());

        BigDecimal refundAmount = calculateRefundAmount(order);
        BigDecimal netAmount = calculateNetAmount(order.getTotalAmount(), refundAmount);

        return SalesLedgerOrderDetailResponse.from(order, items, refundAmount, netAmount);
    }

    private void validateSearchPeriod(OffsetDateTime from, OffsetDateTime to) {
        if (from.isAfter(to)) {
            throw new SalesOrderException(SalesOrderErrorCode.INVALID_SALES_LEDGER_PERIOD);
        }
    }

    private Map<Long, Long> getItemCountMap(List<SalesOrder> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> orderIds = orders.stream()
                .map(SalesOrder::getSalesOrderId)
                .toList();

        return salesOrderItemRepository.countItemsBySalesOrderIds(orderIds);
    }

    private BigDecimal calculateRefundAmount(SalesOrder order) {
        if (order.getStatus() == SalesOrderStatus.REFUNDED) {
            return order.getTotalAmount();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateNetAmount(BigDecimal totalAmount, BigDecimal refundAmount) {
        return totalAmount.subtract(refundAmount);
    }
}
