package kr.inventory.domain.stock.service;

import kr.inventory.domain.document.entity.Document;
import kr.inventory.domain.document.exception.DocumentError;
import kr.inventory.domain.document.exception.DocumentException;
import kr.inventory.domain.document.repository.DocumentRepository;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.repository.PurchaseOrderRepository;
import kr.inventory.domain.stock.controller.dto.request.ManualInboundRequest;
import kr.inventory.domain.stock.controller.dto.request.StockInboundRequest;
import kr.inventory.domain.stock.controller.dto.response.StockInboundItemResponse;
import kr.inventory.domain.stock.controller.dto.response.StockInboundListResponse;
import kr.inventory.domain.stock.controller.dto.response.StockInboundResponse;
import kr.inventory.domain.stock.entity.IngredientStockBatch;
import kr.inventory.domain.stock.entity.StockInbound;
import kr.inventory.domain.stock.entity.StockInboundItem;
import kr.inventory.domain.stock.entity.StockLog;
import kr.inventory.domain.stock.entity.enums.InboundStatus;
import kr.inventory.domain.stock.entity.enums.ResolutionStatus;
import kr.inventory.domain.stock.exception.StockErrorCode;
import kr.inventory.domain.stock.exception.StockException;
import kr.inventory.domain.stock.repository.IngredientStockBatchRepository;
import kr.inventory.domain.stock.repository.StockInboundItemRepository;
import kr.inventory.domain.stock.repository.StockInboundRepository;
import kr.inventory.domain.stock.repository.StockLogRepository;
import kr.inventory.domain.stock.normalization.service.IngredientResolutionService;
import kr.inventory.domain.stock.service.command.StockInboundLogCommand;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.repository.VendorRepository;
import kr.inventory.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import kr.inventory.domain.stock.repository.dto.InboundItemAggregate;

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
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final IngredientResolutionService ingredientResolutionService;

    public StockInboundResponse createManualInbound(Long userId, UUID storePublicId, ManualInboundRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.STORE_NOT_FOUND));

        Vendor vendor = request.vendorId() != null ? vendorRepository.findById(request.vendorId())
                .orElseThrow(() -> new StockException(StockErrorCode.VENDOR_NOT_FOUND)) : null;

        StockInbound inbound = StockInbound.create(store, vendor, null, null, request.inboundDate());
        stockInboundRepository.save(inbound);

        List<StockInboundItem> items = request.items().stream()
                .map(itemDto -> StockInboundItem.createRaw(
                        inbound,
                        itemDto.rawProductName(),
                        itemDto.quantity(),
                        itemDto.unitCost(),
                        itemDto.expirationDate(),
                        itemDto.specText()
                )).toList();

        stockInboundItemRepository.saveAll(items);

        List<StockInboundItemResponse> itemResponses = items.stream()
                .map(StockInboundItemResponse::from)
                .toList();

        return StockInboundResponse.from(inbound, itemResponses);
    }

    public StockInboundResponse createInboundFromDocument(Long userId, UUID storePublicId, StockInboundRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.STORE_NOT_FOUND));

        Vendor vendor = request.vendorId() != null ? vendorRepository.findById(request.vendorId())
                .orElseThrow(() -> new StockException(StockErrorCode.VENDOR_NOT_FOUND)) : null;

        Document sourceDocument = documentRepository.findById(request.sourceDocumentId())
                .orElseThrow(() -> new DocumentException(DocumentError.DOCUMENT_NOT_FOUND));

        PurchaseOrder sourcePurchaseOrder = null;
        if (request.sourcePurchaseOrderId() != null) {
            sourcePurchaseOrder = purchaseOrderRepository.findById(request.sourcePurchaseOrderId())
                    .orElseThrow(() -> new StockException(StockErrorCode.PURCHASE_ORDER_NOT_FOUND));
        }

        StockInbound inbound = StockInbound.create(store, vendor, sourceDocument, sourcePurchaseOrder, request.inboundDate());
        stockInboundRepository.save(inbound);

        List<StockInboundItem> items = request.items().stream()
                .map(itemDto -> StockInboundItem.createRaw(
                        inbound,
                        itemDto.rawProductName(),
                        itemDto.quantity(),
                        itemDto.unitCost(),
                        itemDto.expirationDate(),
                        itemDto.specText()
                ))
                .toList();

        stockInboundItemRepository.saveAll(items);

        List<StockInboundItemResponse> itemResponses = items.stream()
                .map(StockInboundItemResponse::from)
                .toList();

        return StockInboundResponse.from(inbound, itemResponses);
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

        List<StockInboundItem> items = stockInboundItemRepository.findByInboundInboundId(inbound.getInboundId());

        boolean hasUnresolvedItems = items.stream()
                .anyMatch(item -> item.getResolutionStatus() == ResolutionStatus.FAILED);

        if (hasUnresolvedItems) {
            throw new StockException(StockErrorCode.INBOUND_ITEMS_NOT_RESOLVED);
        }

        inbound.confirm(user);

        // CONFIRMED 상태 항목만 IngredientMapping에 학습
        Store store = inbound.getStore();
        items.stream()
                .filter(item -> item.getResolutionStatus() == ResolutionStatus.CONFIRMED)
                .filter(item -> item.getNormalizedRawKey() != null && item.getIngredient() != null)
                .forEach(item -> ingredientResolutionService.upsertMapping(
                        store,
                        storeId,
                        item.getNormalizedRawKey(),
                        item.getIngredient()
                ));

        for (StockInboundItem item : items) {
            IngredientStockBatch batch = IngredientStockBatch.createFromInbound(
                    item.getIngredient(),
                    item
            );
            ingredientStockBatchRepository.save(batch);

            BigDecimal balanceAfter = ingredientStockBatchRepository.calculateTotalQuantity(
                    storeId,
                    item.getIngredient().getIngredientId()
            );

            StockInboundLogCommand command = StockInboundLogCommand.ofInbound(
                    inbound,
                    item,
                    batch,
                    balanceAfter,
                    user
            );

            StockLog log = StockLog.createInboundLog(command);
            stockLogRepository.save(log);
        }
    }

    @Transactional(readOnly = true)
    public StockInboundResponse getInbound(Long userId, UUID storePublicId, UUID inboundPublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        StockInbound inbound = stockInboundRepository.findByInboundPublicIdAndStoreStoreId(inboundPublicId, storeId)
                .orElseThrow(() -> new StockException(StockErrorCode.INBOUND_NOT_FOUND));

        List<StockInboundItem> items = stockInboundItemRepository.findByInboundInboundId(inbound.getInboundId());
        List<StockInboundItemResponse> itemResponses = items.stream()
                .map(StockInboundItemResponse::from)
                .toList();

        return StockInboundResponse.from(inbound, itemResponses);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockInboundListResponse> getInbounds(
            Long userId,
            UUID storePublicId,
            Pageable pageable
    ) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);

        List<InboundStatus> targetStatuses = List.of(InboundStatus.CONFIRMED, InboundStatus.DRAFT);

        Page<StockInbound> inboundPage = stockInboundRepository
                .findByStoreStoreIdAndStatusIn(storeId, targetStatuses, pageable);

        List<Long> inboundIds = inboundPage.getContent().stream()
                .map(StockInbound::getInboundId)
                .toList();

        Map<Long, InboundItemAggregate> aggregates = stockInboundItemRepository
                .findAggregatesByInboundIds(inboundIds)
                .stream()
                .collect(Collectors.toMap(InboundItemAggregate::inboundId, a -> a));

        Page<StockInboundListResponse> dtoPage = inboundPage.map(inbound -> {
            InboundItemAggregate aggregate = aggregates.getOrDefault(
                    inbound.getInboundId(),
                    InboundItemAggregate.empty(inbound.getInboundId())
            );

            return StockInboundListResponse.from(
                    inbound,
                    aggregate.itemCount(),
                    aggregate.unresolvedItemCount(),
                    aggregate.totalCost()
            );
        });

        return PageResponse.from(dtoPage);
    }
}
