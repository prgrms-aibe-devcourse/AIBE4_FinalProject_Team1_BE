package kr.inventory.domain.sales.repository;

import kr.inventory.domain.sales.controller.dto.request.SalesOrderSearchRequest;
import kr.inventory.domain.sales.controller.dto.response.SalesLedgerTotalSummaryResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.domain.sales.service.command.SalesLedgerQueryCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepositoryCustom {
    Optional<SalesOrder> findByIdAndStoreStoreIdWithLock(Long salesOrderId, Long storeId);

    Optional<SalesOrder> findByOrderPublicIdWithItems(UUID orderPublicId, Long storeId);

    Page<SalesOrder> findStoreOrders(
            Long storeId,
            SalesOrderSearchRequest request,
            Pageable pageable
    );

    Page<SalesOrder> findSalesLedgerOrders(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            SalesOrderStatus status,
            SalesOrderType type,
            Pageable pageable
    );

    Page<SalesOrder> findSalesLedgerOrders(
            Long storeId,
            SalesLedgerQueryCondition condition,
            Pageable pageable
    );

    SalesLedgerTotalSummaryResponse calculateSalesLedgerSummary(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            SalesOrderStatus status,
            SalesOrderType type
    );

    SalesLedgerTotalSummaryResponse calculateSalesLedgerSummary(
            Long storeId,
            SalesLedgerQueryCondition condition
    );
}
