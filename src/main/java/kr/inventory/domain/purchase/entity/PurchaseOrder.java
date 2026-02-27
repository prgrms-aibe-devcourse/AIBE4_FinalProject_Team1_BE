package kr.inventory.domain.purchase.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.purchase.entity.enums.PurchaseOrderStatus;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.vendor.entity.Vendor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "purchase_order",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_purchase_order_order_no", columnNames = "order_no")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long purchaseOrderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(name = "order_no", length = 40)
    private String orderNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    private Long submittedByUserId;

    private OffsetDateTime submittedAt;

    private Long confirmedByUserId;

    private OffsetDateTime confirmedAt;

    private Long canceledByUserId;

    private OffsetDateTime canceledAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PurchaseOrderItem> items = new ArrayList<>();

    public static PurchaseOrder createDraft(Store store) {
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.store = store;
        purchaseOrder.status = PurchaseOrderStatus.DRAFT;
        purchaseOrder.totalAmount = BigDecimal.ZERO;
        return purchaseOrder;
    }

    public void assignVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public void replaceItems(List<PurchaseOrderItem> newItems) {
        items.clear();
        for (PurchaseOrderItem newItem : newItems) {
            newItem.assignOrder(this);
            items.add(newItem);
        }
        recalculateTotalAmount();
    }

    public void submit(String orderNo, Long submittedByUserId, OffsetDateTime submittedAt) {
        this.orderNo = orderNo;
        this.status = PurchaseOrderStatus.SUBMITTED;
        this.submittedByUserId = submittedByUserId;
        this.submittedAt = submittedAt;
    }

    public void confirm(Long confirmedByUserId, OffsetDateTime confirmedAt) {
        this.status = PurchaseOrderStatus.CONFIRMED;
        this.confirmedByUserId = confirmedByUserId;
        this.confirmedAt = confirmedAt;
    }

    public void cancel(Long canceledByUserId, OffsetDateTime canceledAt) {
        this.status = PurchaseOrderStatus.CANCELED;
        this.canceledByUserId = canceledByUserId;
        this.canceledAt = canceledAt;
    }

    private void recalculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(PurchaseOrderItem::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
