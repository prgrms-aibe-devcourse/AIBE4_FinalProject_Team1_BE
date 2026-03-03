package kr.inventory.domain.sales.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.dining.entity.DiningTable;
import kr.inventory.domain.dining.entity.TableSession;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.sales.entity.enums.SalesOrderType;
import kr.inventory.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "sales_orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sales_order_store_idempotency",
                columnNames = {"store_id", "idempotency_key"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long salesOrderId;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID orderPublicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_session_id")
    private TableSession tableSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dining_table_id", nullable = false)
    private DiningTable diningTable;

    @Column(nullable = false, length = 64)
    private String idempotencyKey;

    @Column(nullable = false)
    private OffsetDateTime orderedAt;

    @Column
    private OffsetDateTime completedAt;

    @Column
    private OffsetDateTime refundedAt;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SalesOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SalesOrderType type;

    @Column(nullable = false)
    private boolean stockProcessed = false;

    @Column
    private OffsetDateTime stockProcessedAt;

    /**
     * QR 주문 생성
     */
    public static SalesOrder create(
            Store store,
            DiningTable diningTable,
            TableSession tableSession,
            String idempotencyKey,
            SalesOrderType type
    ) {
        SalesOrder order = new SalesOrder();
        order.store = store;
        order.diningTable = diningTable;
        order.tableSession = tableSession;
        order.idempotencyKey = idempotencyKey;
        order.orderedAt = OffsetDateTime.now(ZoneOffset.UTC);
        order.type = type;
        order.status = SalesOrderStatus.COMPLETED;
        order.totalAmount = BigDecimal.ZERO;
        return order;
    }

    public void updateTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void updateStatus(SalesOrderStatus newStatus) {
        this.status = newStatus;
    }

    public void updateCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void updateRefundedAt(OffsetDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public void markAsStockProcessed() {
        if (this.stockProcessed) {
            return;
        }
        this.stockProcessed = true;
        this.stockProcessedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void rollbackStockProcessed() {
        this.stockProcessed = false;
        this.stockProcessedAt = null;
    }
}