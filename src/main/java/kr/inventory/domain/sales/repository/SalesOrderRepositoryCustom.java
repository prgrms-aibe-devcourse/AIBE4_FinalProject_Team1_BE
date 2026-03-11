package kr.inventory.domain.sales.repository;

import kr.inventory.domain.sales.controller.dto.response.SalesOrderResponse;
import kr.inventory.domain.sales.entity.SalesOrder;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepositoryCustom {
    Optional<SalesOrder> findByIdAndStoreStoreIdWithLock(Long salesOrderId, Long storeId);

    Optional<SalesOrder> findByOrderPublicIdWithItems(UUID orderPublicId, Long storeId);

    Page<SalesOrder> findStoreOrders(Long storeId, Pageable pageable);

    Page<SalesOrder> findSalesLedgerOrders(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            SalesOrderStatus status,
            SalesOrderType type,
            Pageable pageable
    );

    kr.inventory.domain.sales.controller.dto.response.SalesLedgerTotalSummaryResponse calculateSalesLedgerSummary(
            Long storeId,
            OffsetDateTime from,
            OffsetDateTime to,
            SalesOrderStatus status,
            SalesOrderType type
    );
}
