package kr.inventory.domain.stock.service;

import kr.inventory.domain.catalog.repository.IngredientRepository;
import kr.inventory.domain.document.entity.Document;
import kr.inventory.domain.document.exception.DocumentError;
import kr.inventory.domain.document.exception.DocumentException;
import kr.inventory.domain.document.repository.DocumentRepository;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.repository.PurchaseOrderRepository;
import kr.inventory.domain.stock.controller.dto.request.StockInboundRequest;
import kr.inventory.domain.stock.controller.dto.response.StockInboundResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.StockLog;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StockInboundItemRepository;
import kr.inventory.domain.stock.repository.StockInboundRepository;
import kr.inventory.domain.stock.repository.StockLogRepository;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StockInboundService {

	private final StockInboundRepository stockInboundRepository;
	private final StockInboundItemRepository stockInboundItemRepository;
	private final IngredientStockBatchRepository ingredientStockBatchRepository;
	private final StockLogRepository stockLogRepository;
	private final StoreAccessValidator storeAccessValidator;
	private final StoreRepository storeRepository;
	private final VendorRepository vendorRepository;
	private final IngredientRepository ingredientRepository;
	private final UserRepository userRepository;
	private final DocumentRepository documentRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;

	public StockInboundResponse createInbound(Long userId, UUID storePublicId, StockInboundRequest request) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new StockException(StockErrorCode.STORE_NOT_FOUND));

		Vendor selectedVendor = request.vendorId() == null ? null : findActiveVendorOrThrow(request.vendorId());

		Document sourceDocument = documentRepository.findById(request.sourceDocumentId())
			.orElseThrow(() -> new DocumentException(
				DocumentError.DOCUMENT_NOT_FOUND));

		PurchaseOrder sourcePurchaseOrder = purchaseOrderRepository.findById(request.sourcePurchaseOrderId())
			.orElseThrow();

		validatePurchaseOrderStore(sourcePurchaseOrder, storeId);

		Vendor vendor = resolveInboundVendor(selectedVendor, sourcePurchaseOrder);

		StockInbound inbound = StockInbound.create(store, vendor, sourceDocument, sourcePurchaseOrder);
		stockInboundRepository.save(inbound);

		List<StockInboundItem> items = request.items().stream()
			.map(itemDto -> {
				// TODO: ingredientRepository 에서 ingredient 를 찾지 못했을 경우 예외처리 필요
				return StockInboundItem.create(
					inbound,
					ingredientRepository.findById(itemDto.ingredientId()).orElseThrow(),
					itemDto.quantity(),
					itemDto.unitCost(),
					itemDto.expirationDate()
				);
			}).toList();

		stockInboundItemRepository.saveAll(items);

		return StockInboundResponse.from(inbound);
	}

	private Vendor findActiveVendorOrThrow(Long vendorId) {
		Vendor vendor = vendorRepository.findById(vendorId)
			.orElseThrow(() -> new StockException(StockErrorCode.VENDOR_NOT_FOUND));

		if (vendor.getStatus() != VendorStatus.ACTIVE) {
			throw new StockException(StockErrorCode.VENDOR_NOT_ACTIVE);
		}

		return vendor;
	}

	private void validatePurchaseOrderStore(PurchaseOrder purchaseOrder, Long storeId) {
		if (!purchaseOrder.getStore().getStoreId().equals(storeId)) {
			throw new StockException(StockErrorCode.PURCHASE_ORDER_STORE_MISMATCH);
		}
	}

	private Vendor resolveInboundVendor(Vendor selectedVendor, PurchaseOrder sourcePurchaseOrder) {
		Vendor purchaseOrderVendor = sourcePurchaseOrder.getVendor();

		if (purchaseOrderVendor == null) {
			return selectedVendor;
		}

		if (purchaseOrderVendor.getStatus() != VendorStatus.ACTIVE) {
			throw new StockException(StockErrorCode.VENDOR_NOT_ACTIVE);
		}

		if (selectedVendor != null && !purchaseOrderVendor.getVendorId().equals(selectedVendor.getVendorId())) {
			throw new StockException(StockErrorCode.PURCHASE_ORDER_VENDOR_MISMATCH);
		}

		return purchaseOrderVendor;
	}

	@Transactional(readOnly = true)
	public StockInboundResponse getInbound(UUID inboundPublicId) {
		StockInbound inbound = stockInboundRepository.findByInboundPublicId(inboundPublicId)
			.orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));
		return StockInboundResponse.from(inbound);
	}

	@Transactional(readOnly = true)
	public Page<StockInboundResponse> getInbounds(Long storeId, Pageable pageable) {
		return stockInboundRepository.findByStoreStoreId(storeId, pageable)
			.map(StockInboundResponse::from);
	}

	public void confirmInbound(UUID inboundPublicId, Long userId) {
		StockInbound inbound = stockInboundRepository.findByInboundPublicId(inboundPublicId)
			.orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

		if (inbound.getStatus() != InboundStatus.DRAFT) {
			throw new StockException(StockErrorCode.INBOUND_NOT_DRAFT_STATUS);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new StockException(StockErrorCode.USER_NOT_FOUND));

		inbound.confirm(user);

		List<StockInboundItem> items = stockInboundItemRepository.findByInbound_InboundId(inbound.getInboundId());

		for (StockInboundItem item : items) {
			IngredientStockBatch batch = IngredientStockBatch.createFromInbound(
				inbound.getStore(),
				item.getIngredient(),
				item
			);
			ingredientStockBatchRepository.save(batch);

			StockLog log = StockLog.createInboundLog(
				inbound.getStore(),
				item.getIngredient(),
				item.getQuantity(),
				batch,
				inbound.getInboundId(),
				user
			);
			stockLogRepository.save(log);
		}
	}

	public void deleteInbound(UUID inboundPublicId) {
		StockInbound inbound = stockInboundRepository.findByInboundPublicId(inboundPublicId)
			.orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

		if (inbound.getStatus() != InboundStatus.DRAFT) {
			throw new StockException(StockErrorCode.INBOUND_NOT_DRAFT_STATUS);
		}

		stockInboundItemRepository.deleteAllByInbound_InboundId(inbound.getInboundId());
		stockInboundRepository.delete(inbound);
	}
}
