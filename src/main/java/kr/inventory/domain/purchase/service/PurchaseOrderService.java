package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.constant.PurchaseOrderConstant;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderSearchRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderSummaryResponse;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.entity.PurchaseOrderItem;
import kr.inventory.domain.purchase.exception.PurchaseOrderErrorCode;
import kr.inventory.domain.purchase.exception.PurchaseOrderException;
import kr.inventory.domain.purchase.repository.PurchaseOrderItemRepository;
import kr.inventory.domain.purchase.repository.PurchaseOrderRepository;
import kr.inventory.domain.purchase.validator.PurchaseOrderValidator;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.user.entity.User;
import kr.inventory.domain.user.repository.UserRepository;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.repository.VendorRepository;
import kr.inventory.global.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private static final DateTimeFormatter ORDER_NO_DATE_FORMATTER = DateTimeFormatter
            .ofPattern(PurchaseOrderConstant.ORDER_NO_DATE_PATTERN);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final StoreRepository storeRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final PurchaseOrderPdfService purchaseOrderPdfService;
    private final PurchaseOrderValidator purchaseOrderValidator;
    private final StoreAccessValidator storeAccessValidator;

    @Transactional
    public PurchaseOrderDetailResponse create(Long userId, UUID storePublicId, PurchaseOrderCreateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreIdForActiveMembers(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new PurchaseOrderException(PurchaseOrderErrorCode.STORE_NOT_FOUND));

        purchaseOrderValidator.requireItemsNotEmpty(request.items());
        Vendor vendor = resolveVendorOrThrow(storeId, request.vendorPublicId());

        PurchaseOrder purchaseOrder = PurchaseOrder.create(store);
        purchaseOrder.assignVendor(vendor);
        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        String orderNo = generateOrderNumber(savedPurchaseOrder.getPurchaseOrderId(),
                OffsetDateTime.now(ZoneOffset.UTC));
        savedPurchaseOrder.assignOrderNo(orderNo);

        List<PurchaseOrderItem> items = request.items().stream()
                .map(req -> PurchaseOrderItem.create(req.itemName(), req.quantity(), req.unit(), req.unitPrice()))
                .toList();
        items.forEach(item -> item.assignOrder(savedPurchaseOrder));
        List<PurchaseOrderItem> savedItems = purchaseOrderItemRepository.saveAll(items);

        BigDecimal totalAmount = savedItems.stream()
                .map(PurchaseOrderItem::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        savedPurchaseOrder.updateTotalAmount(totalAmount);

        return PurchaseOrderDetailResponse.from(savedPurchaseOrder, savedItems, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderSummaryResponse> getPurchaseOrders(
            Long userId,
            UUID storePublicId,
            PurchaseOrderSearchRequest searchRequest,
            Pageable pageable) {
        Long storeId = storeAccessValidator.validateAndGetStoreIdForActiveMembers(userId, storePublicId);
        Page<PurchaseOrder> page = purchaseOrderRepository.findByStoreIdWithFilters(storeId, searchRequest, pageable);
        return PageResponse.from(page.map(PurchaseOrderSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDetailResponse getPurchaseOrder(Long userId, UUID storePublicId, UUID purchaseOrderPublicId) {
        PurchaseOrder purchaseOrder = validateAndGetPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);
        List<PurchaseOrderItem> items = purchaseOrderItemRepository
                .findByPurchaseOrderPurchaseOrderId(purchaseOrder.getPurchaseOrderId());
        UUID canceledByUserPublicId = getCanceledByUserPublicId(purchaseOrder.getCanceledByUserId());
        return PurchaseOrderDetailResponse.from(purchaseOrder, items, canceledByUserPublicId);
    }

    @Transactional
    public PurchaseOrderDetailResponse update(Long userId, UUID storePublicId, UUID purchaseOrderPublicId,
            PurchaseOrderUpdateRequest request) {
        PurchaseOrder purchaseOrder = validateAndGetPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);
        purchaseOrderValidator.requireCancelable(purchaseOrder.getStatus());
        purchaseOrderValidator.requireItemsNotEmpty(request.items());

        Vendor vendor = resolveVendorOrThrow(purchaseOrder.getStore().getStoreId(), request.vendorPublicId());
        purchaseOrder.assignVendor(vendor);

        List<PurchaseOrderItem> oldItems = purchaseOrderItemRepository
                .findByPurchaseOrderPurchaseOrderId(purchaseOrder.getPurchaseOrderId());
        purchaseOrderItemRepository.deleteAllInBatch(oldItems);

        List<PurchaseOrderItem> newItems = request.items().stream()
                .map(req -> PurchaseOrderItem.create(req.itemName(), req.quantity(), req.unit(), req.unitPrice()))
                .toList();
        newItems.forEach(item -> item.assignOrder(purchaseOrder));
        List<PurchaseOrderItem> savedItems = purchaseOrderItemRepository.saveAll(newItems);

        BigDecimal totalAmount = savedItems.stream()
                .map(PurchaseOrderItem::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        purchaseOrder.updateTotalAmount(totalAmount);

        UUID canceledByUserPublicId = getCanceledByUserPublicId(purchaseOrder.getCanceledByUserId());
        return PurchaseOrderDetailResponse.from(purchaseOrder, savedItems, canceledByUserPublicId);
    }

    @Transactional
    public PurchaseOrderDetailResponse cancel(Long userId, UUID storePublicId, UUID purchaseOrderPublicId) {
        PurchaseOrder purchaseOrder = validateAndGetPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);
        purchaseOrderValidator.requireCancelable(purchaseOrder.getStatus());
        purchaseOrder.cancel(userId, OffsetDateTime.now(ZoneOffset.UTC));

        UUID canceledByUserPublicId = getCanceledByUserPublicId(userId);
        List<PurchaseOrderItem> items = purchaseOrderItemRepository
                .findByPurchaseOrderPurchaseOrderId(purchaseOrder.getPurchaseOrderId());
        return PurchaseOrderDetailResponse.from(purchaseOrder, items, canceledByUserPublicId);
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(Long userId, UUID storePublicId, UUID purchaseOrderPublicId) {
        PurchaseOrder purchaseOrder = validateAndGetPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);
        return purchaseOrderPdfService.generate(purchaseOrder);
    }

    private PurchaseOrder validateAndGetPurchaseOrder(Long userId, UUID storePublicId, UUID purchaseOrderPublicId) {
        Long purchaseOrderId = purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId,
                purchaseOrderPublicId);
        return purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new PurchaseOrderException(PurchaseOrderErrorCode.PURCHASE_ORDER_NOT_FOUND));
    }

    private Vendor resolveVendorOrThrow(Long storeId, UUID vendorPublicId) {
        Vendor vendor = vendorRepository.findByVendorPublicId(vendorPublicId)
                .orElseThrow(() -> new PurchaseOrderException(PurchaseOrderErrorCode.VENDOR_NOT_FOUND));
        if (!vendor.getStore().getStoreId().equals(storeId)) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.VENDOR_STORE_MISMATCH);
        }
        if (vendor.getStatus() != VendorStatus.ACTIVE) {
            throw new PurchaseOrderException(PurchaseOrderErrorCode.VENDOR_NOT_ACTIVE);
        }
        return vendor;
    }

    private UUID getCanceledByUserPublicId(Long canceledByUserId) {
        if (canceledByUserId == null) {
            return null;
        }
        return userRepository.findById(canceledByUserId)
                .map(User::getPublicId)
                .orElse(null);
    }

    private String generateOrderNumber(Long purchaseOrderId, OffsetDateTime orderedAt) {
        String datePart = orderedAt.format(ORDER_NO_DATE_FORMATTER);
        String sequencePart = String.format(PurchaseOrderConstant.ORDER_NO_SEQUENCE_FORMAT, purchaseOrderId);
        return String.join(
                PurchaseOrderConstant.ORDER_NO_SEPARATOR,
                PurchaseOrderConstant.ORDER_NO_PREFIX,
                datePart,
                sequencePart);
    }
}
