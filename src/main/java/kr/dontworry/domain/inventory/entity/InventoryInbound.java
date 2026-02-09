package kr.dontworry.domain.inventory.entity;

import jakarta.persistence.*;
import kr.dontworry.domain.common.AuditableEntity;
import kr.dontworry.domain.document.entity.Document;
import kr.dontworry.domain.inventory.entity.enums.InboundSourceType;
import kr.dontworry.domain.inventory.entity.enums.InboundStatus;
import kr.dontworry.domain.purchase.entity.PurchaseOrder;
import kr.dontworry.domain.store.entity.Store;
import kr.dontworry.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "inventory_inbounds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryInbound extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inboundId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InboundSourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id")
    private Document sourceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_purchase_order_id")
    private PurchaseOrder sourcePurchaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InboundStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_user_id")
    private User confirmedByUser;

    private OffsetDateTime confirmedAt;

    public static InventoryInbound create(Store store, InboundSourceType sourceType) {
        InventoryInbound inbound = new InventoryInbound();
        inbound.store = store;
        inbound.sourceType = sourceType;
        inbound.status = InboundStatus.DRAFT;
        return inbound;
    }
}
