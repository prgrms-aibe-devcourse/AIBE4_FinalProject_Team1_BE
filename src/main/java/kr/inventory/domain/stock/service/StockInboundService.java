package kr.inventory.domain.stock.service;

import kr.inventory.domain.reference.entity.Ingredient;
import kr.inventory.domain.reference.repository.IngredientRepository;
import kr.inventory.domain.document.entity.Document;
import kr.inventory.domain.document.exception.DocumentError;
import kr.inventory.domain.document.exception.DocumentException;
import kr.inventory.domain.document.repository.DocumentRepository;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.repository.PurchaseOrderRepository;
import kr.inventory.domain.stock.controller.dto.request.StockInboundRequest;
import kr.inventory.domain.stock.controller.dto.response.StockInboundItemResponse;
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
import kr.inventory.domain.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StockInboundService {

	private final StockInboundRepository stockInboundRepository;
	private final StockInboundItemRepository stockInboundItemRepository;
	private final StoreAccessValidator storeAccessValidator;
	private final StoreRepository storeRepository;
	private final VendorRepository vendorRepository;
	private final IngredientRepository ingredientRepository;
	private final UserRepository userRepository;
	private final DocumentRepository documentRepository;
	private final PurchaseOrderRepository purchaseOrderRepository;
	private final StockService stockService;

	public StockInboundResponse createInbound(Long userId, UUID storePublicId, StockInboundRequest request) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new StockException(StockErrorCode.STORE_NOT_FOUND));

		Vendor vendor = vendorRepository.findById(request.vendorId())
			.orElseThrow(() -> new StockException(StockErrorCode.VENDOR_NOT_FOUND));

		Document sourceDocument = documentRepository.findById(request.sourceDocumentId())
			.orElseThrow(() -> new DocumentException(
				DocumentError.DOCUMENT_NOT_FOUND));

		PurchaseOrder sourcePurchaseOrder = purchaseOrderRepository.findById(request.sourcePurchaseOrderId())
			.orElseThrow();

		StockInbound inbound = StockInbound.create(store, vendor, sourceDocument, sourcePurchaseOrder);
		stockInboundRepository.save(inbound);

		List<StockInboundItem> items = request.items().stream()
			.map(itemDto -> {
				// TODO 마스터 테이블 경규화
				return StockInboundItem.create(
					inbound,
					itemDto.rawProductName(),
					itemDto.quantity(),
					itemDto.unitCost(),
					itemDto.expirationDate()
				);
			}).toList();

		stockInboundItemRepository.saveAll(items);

		return StockInboundResponse.fromEntity(inbound, items);
	}

	@Transactional(readOnly = true)
	public StockInboundResponse getInbound(Long userId, UUID storePublicId, UUID inboundPublicId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		return stockInboundRepository.findInboundWithItems(inboundPublicId, storeId)
			.orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public Page<StockInboundResponse> getInbounds(Long userId, UUID storePublicId, Pageable pageable) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		return stockInboundRepository.findByStoreStoreId(storeId, pageable)
			.map(inbound -> StockInboundResponse.from(inbound, List.of()));
	}

	public void confirmInbound(Long userId, UUID storePublicId, UUID inboundPublicId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

		StockInbound inbound = stockInboundRepository.findByInboundPublicIdAndStoreStoreId(inboundPublicId, storeId)
			.orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

		if (inbound.getStatus() != InboundStatus.DRAFT) {
			throw new StockException(StockErrorCode.INBOUND_NOT_DRAFT_STATUS);
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new StockException(StockErrorCode.USER_NOT_FOUND));

		inbound.confirm(user);

		List<StockInboundItem> items = stockInboundItemRepository.findByInbound_InboundId(inbound.getInboundId());

		stockService.registerInboundStock(items);
	}

	public void deleteInbound(Long userId, UUID storePublicId, UUID inboundPublicId) {
		Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
		StockInbound inbound = stockInboundRepository.findByInboundPublicIdAndStoreStoreId(inboundPublicId, storeId)
			.orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

		if (inbound.getStatus() != InboundStatus.DRAFT) {
			throw new StockException(StockErrorCode.INBOUND_NOT_DRAFT_STATUS);
		}

		stockInboundItemRepository.deleteAllByInbound_InboundId(inbound.getInboundId());
		stockInboundRepository.delete(inbound);
	}
}
