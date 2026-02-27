package kr.inventory.domain.stock.entity;

import jakarta.persistence.*;
import kr.inventory.domain.common.AuditableEntity;
import kr.inventory.domain.document.entity.Document;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "stock_inbounds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockInbound extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long inboundId;

	@Column(nullable = false, updatable = false)
	private UUID inboundPublicId = UUID.randomUUID();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vendor_id")
	private Vendor vendor;

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

	public static StockInbound create(Store store, Vendor vendor, Document sourceDocument,
		PurchaseOrder sourcePurchaseOrder) {
		StockInbound inbound = new StockInbound();
		inbound.store = store;
		inbound.vendor = vendor;
		inbound.sourceDocument = sourceDocument;
		inbound.sourcePurchaseOrder = sourcePurchaseOrder;
		inbound.status = InboundStatus.DRAFT;
		return inbound;
	}

	public void confirm(User confirmedByUser) {
		this.status = InboundStatus.CONFIRMED;
		this.confirmedByUser = confirmedByUser;
		this.confirmedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}
}
