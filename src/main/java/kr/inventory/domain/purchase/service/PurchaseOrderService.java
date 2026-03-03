package kr.inventory.domain.purchase.service;

import kr.inventory.domain.purchase.constant.PurchaseOrderConstant;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
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
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.store.service.StoreAccessValidator;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
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
/** 발주서의 생성·상태 전이·PDF 다운로드를 처리하는 서비스다. */
public class PurchaseOrderService {

    private static final DateTimeFormatter ORDER_NO_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(PurchaseOrderConstant.ORDER_NO_DATE_PATTERN);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final StoreRepository storeRepository;
    private final PurchaseOrderPdfService purchaseOrderPdfService;
    private final PurchaseOrderValidator purchaseOrderValidator;
    private final VendorRepository vendorRepository;
    private final StoreAccessValidator storeAccessValidator;

    @Transactional
    public PurchaseOrderDetailResponse createDraft(Long userId, UUID storePublicId, PurchaseOrderCreateRequest request) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new PurchaseOrderException(PurchaseOrderErrorCode.STORE_NOT_FOUND));
        purchaseOrderValidator.requireItemsNotEmpty(request.items());
        Vendor vendor = resolveVendorOrThrow(storeId, request.vendorPublicId());

        PurchaseOrder purchaseOrder = PurchaseOrder.createDraft(store);
        purchaseOrder.assignVendor(vendor);
        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderItem> items = request.items().stream()
                .map(req -> PurchaseOrderItem.create(req.itemName(), req.quantity(), req.unitPrice()))
                .toList();

        for (PurchaseOrderItem item : items) {
            item.assignOrder(savedPurchaseOrder);
        }
        List<PurchaseOrderItem> savedItems = purchaseOrderItemRepository.saveAll(items);

        return PurchaseOrderDetailResponse.from(savedPurchaseOrder, savedItems);
    }


    @Transactional(readOnly = true)
    /** 특정 매장의 발주서 목록을 최신순으로 조회한다. */
    public List<PurchaseOrderSummaryResponse> getPurchaseOrders(Long userId, UUID storePublicId) {
        Long storeId = storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        return purchaseOrderRepository.findAllByStoreStoreIdOrderByPurchaseOrderIdDesc(storeId).stream()
                .map(PurchaseOrderSummaryResponse::from)
                .toList();
    }


    @Transactional(readOnly = true)
    /** 단건 발주서 상세 정보를 조회한다. */
    public PurchaseOrderDetailResponse getPurchaseOrder(Long userId, UUID storePublicId, UUID purchaseOrderPublicId) {
        PurchaseOrder purchaseOrder = validateAndGetPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(purchaseOrder.getPurchaseOrderId());
        return PurchaseOrderDetailResponse.from(purchaseOrder, items);
    }

    @Transactional
    public PurchaseOrderDetailResponse updateDraft(Long userId, UUID storePublicId, UUID purchaseOrderPublicId, PurchaseOrderUpdateRequest request) {
        PurchaseOrder purchaseOrder = validateAndGetPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);
        purchaseOrderValidator.requireItemsNotEmpty(request.items());

        purchaseOrderValidator.requireDraftForUpdate(purchaseOrder.getStatus());
        Vendor vendor = resolveVendorOrThrow(purchaseOrder.getStore().getStoreId(), request.vendorPublicId());

        purchaseOrder.assignVendor(vendor);

        List<PurchaseOrderItem> oldItems = purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(purchaseOrder.getPurchaseOrderId());
        purchaseOrderItemRepository.deleteAll(oldItems);

        List<PurchaseOrderItem> newItems = request.items().stream()
                .map(req -> PurchaseOrderItem.create(req.itemName(), req.quantity(), req.unitPrice()))
                .toList();

        for (PurchaseOrderItem item : newItems) {
            item.assignOrder(purchaseOrder);
        }
        List<PurchaseOrderItem> savedItems = purchaseOrderItemRepository.saveAll(newItems);

        return PurchaseOrderDetailResponse.from(purchaseOrder, savedItems);
    }

    @Transactional
    public PurchaseOrderDetailResponse submit(Long userId, UUID storePublicId, UUID purchaseOrderPublicId) {
        PurchaseOrder purchaseOrder = validateAndGetPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);
        purchaseOrderValidator.requireDraftForSubmit(purchaseOrder.getStatus());

        OffsetDateTime submittedAt = OffsetDateTime.now(ZoneOffset.UTC);
        String orderNo = generateOrderNumber(purchaseOrder.getPurchaseOrderId(), submittedAt);
        purchaseOrder.submit(orderNo, userId, submittedAt);

        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(purchaseOrder.getPurchaseOrderId());
        return PurchaseOrderDetailResponse.from(purchaseOrder, items);
    }

    @Transactional
    public PurchaseOrderDetailResponse confirm(Long userId, UUID storePublicId, UUID purchaseOrderPublicId) {
        PurchaseOrder purchaseOrder = validateAndGetPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);
        StoreMemberRole role = purchaseOrderValidator.requireManagerOrAbove(purchaseOrder.getStore().getStoreId(), userId);
        purchaseOrderValidator.requireOwner(role);
        purchaseOrderValidator.requireSubmittedForConfirm(purchaseOrder.getStatus());

        purchaseOrder.confirm(userId, OffsetDateTime.now(ZoneOffset.UTC));

        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(purchaseOrder.getPurchaseOrderId());
        return PurchaseOrderDetailResponse.from(purchaseOrder, items);
    }

    @Transactional
    public PurchaseOrderDetailResponse cancel(Long userId, UUID storePublicId, UUID purchaseOrderPublicId) {
        PurchaseOrder purchaseOrder = validateAndGetPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);
        purchaseOrderValidator.requireCancelable(purchaseOrder.getStatus());

        purchaseOrder.cancel(userId, OffsetDateTime.now(ZoneOffset.UTC));

        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderPurchaseOrderId(purchaseOrder.getPurchaseOrderId());
        return PurchaseOrderDetailResponse.from(purchaseOrder, items);
    }

    @Transactional(readOnly = true)
    /** 발주서 상세를 PDF로 생성해 반환한다. */
    public byte[] downloadPdf(Long userId, UUID storePublicId, UUID purchaseOrderPublicId) {
        PurchaseOrder purchaseOrder = validateAndGetPurchaseOrder(userId, storePublicId, purchaseOrderPublicId);
        return purchaseOrderPdfService.generate(purchaseOrder);
    }

    private PurchaseOrder validateAndGetPurchaseOrder(Long userId, UUID storePublicId, UUID purchaseOrderPublicId) {
        storeAccessValidator.validateAndGetStoreId(userId, storePublicId);
        Long purchaseOrderId = purchaseOrderValidator.validateAccessAndGetPurchaseOrderId(userId, purchaseOrderPublicId);
        return getPurchaseOrderOrThrow(purchaseOrderId);
    }

    private PurchaseOrder getPurchaseOrderOrThrow(Long purchaseOrderId) {
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

    private String generateOrderNumber(Long purchaseOrderId, OffsetDateTime submittedAt) {
        String datePart = submittedAt.format(ORDER_NO_DATE_FORMATTER);
        String sequencePart = String.format(PurchaseOrderConstant.ORDER_NO_SEQUENCE_FORMAT, purchaseOrderId);
        return String.join(
                PurchaseOrderConstant.ORDER_NO_SEPARATOR,
                PurchaseOrderConstant.ORDER_NO_PREFIX,
                datePart,
                sequencePart
        );
    }
}
