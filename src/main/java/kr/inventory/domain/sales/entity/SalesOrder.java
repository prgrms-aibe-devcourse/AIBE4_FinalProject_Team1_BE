package kr.inventory.domain.sales.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.sales.entity.enums.SalesOrderChannel;
import kr.inventory.domain.sales.entity.enums.SalesOrderStatus;
import kr.inventory.domain.store.entity.Store;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "sales_orders",
        uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "external_order_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long salesOrderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 120)
    private String externalOrderId;

    @Column(nullable = false)
    private OffsetDateTime orderedAt;

    private OffsetDateTime paidAt;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SalesOrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SalesOrderChannel channel;

    public static SalesOrder create(
            Store store,
            String externalOrderId,
            OffsetDateTime orderedAt,
            SalesOrderChannel channel
    ) {
        SalesOrder order = new SalesOrder();
        order.store = store;
        order.externalOrderId = externalOrderId;
        order.orderedAt = orderedAt;
        order.channel = channel;
        order.status = SalesOrderStatus.ORDERED;
        order.totalAmount = BigDecimal.ZERO;
        return order;
    }
}
