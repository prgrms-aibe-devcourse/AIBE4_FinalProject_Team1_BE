package kr.inventory.domain.purchase.service.impl;

import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderCreateRequest;
import kr.inventory.domain.purchase.controller.dto.request.PurchaseOrderUpdateRequest;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderDetailResponse;
import kr.inventory.domain.purchase.controller.dto.response.PurchaseOrderSummaryResponse;
import kr.inventory.domain.purchase.entity.PurchaseOrder;
import kr.inventory.domain.purchase.exception.PurchaseOrderErrorCode;
import kr.inventory.domain.purchase.exception.PurchaseOrderException;
import kr.inventory.domain.purchase.factory.PurchaseOrderFactory;
import kr.inventory.domain.purchase.mapper.PurchaseOrderResponseMapper;
import kr.inventory.domain.purchase.repository.PurchaseOrderRepository;
import kr.inventory.domain.purchase.service.PurchaseOrderNumberGenerator;
import kr.inventory.domain.purchase.service.PurchaseOrderPdfService;
import kr.inventory.domain.purchase.service.PurchaseOrderService;
import kr.inventory.domain.purchase.validator.PurchaseOrderValidator;
import kr.inventory.domain.store.entity.Store;
import kr.inventory.domain.store.entity.enums.StoreMemberRole;
import kr.inventory.domain.store.repository.StoreRepository;
import kr.inventory.domain.vendor.entity.Vendor;
import kr.inventory.domain.vendor.entity.enums.VendorStatus;
import kr.inventory.domain.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StoreRepository storeRepository;
    private final PurchaseOrderFactory purchaseOrderFactory;
    private final PurchaseOrderNumberGenerator purchaseOrderNumberGenerator;
    private final PurchaseOrderPdfService purchaseOrderPdfService;
    private final PurchaseOrderValidator purchaseOrderValidator;
    private final PurchaseOrderResponseMapper purchaseOrderResponseMapper;
    private final VendorRepository vendorRepository;

    @Override
    public PurchaseOrderDetailResponse createDraft(Long userId, PurchaseOrderCreateRequest request) {
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new PurchaseOrderException(PurchaseOrderErrorCode.STORE_NOT_FOUND));
        purchaseOrderValidator.requireManagerOrAbove(store.getStoreId(), userId);
        purchaseOrderValidator.requireItemsNotEmpty(request.items());
        Vendor vendor = resolveVendorOrThrow(store.getStoreId(), request.vendorPublicId());

        PurchaseOrder purchaseOrder = purchaseOrderFactory.createDraft(store, request.items());
        purchaseOrder.assignVendor(vendor);
        PurchaseOrder savedPurchaseOrder = purchaseOrderRepository.save(purchaseOrder);
        return purchaseOrderResponseMapper.toDetailResponse(savedPurchaseOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderSummaryResponse> getPurchaseOrders(Long userId, Long storeId) {
        purchaseOrderValidator.requireManagerOrAbove(storeId, userId);
        return purchaseOrderRepository.findAllByStoreStoreIdOrderByPurchaseOrderIdDesc(storeId).stream()
                .map(purchaseOrderResponseMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderDetailResponse getPurchaseOrder(Long userId, Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);
        purchaseOrderValidator.requireManagerOrAbove(purchaseOrder.getStore().getStoreId(), userId);
        return purchaseOrderResponseMapper.toDetailResponse(purchaseOrder);
    }

    @Override
    public PurchaseOrderDetailResponse updateDraft(Long userId, Long purchaseOrderId, PurchaseOrderUpdateRequest request) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);
        purchaseOrderValidator.requireManagerOrAbove(purchaseOrder.getStore().getStoreId(), userId);
        purchaseOrderValidator.requireItemsNotEmpty(request.items());

        purchaseOrderValidator.requireDraftForUpdate(purchaseOrder.getStatus());
        Vendor vendor = resolveVendorOrThrow(purchaseOrder.getStore().getStoreId(), request.vendorPublicId());

        purchaseOrder.assignVendor(vendor);
        purchaseOrder.replaceItems(purchaseOrderFactory.createItems(request.items()));
        return purchaseOrderResponseMapper.toDetailResponse(purchaseOrder);
    }

    @Override
    public PurchaseOrderDetailResponse submit(Long userId, Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);
        purchaseOrderValidator.requireManagerOrAbove(purchaseOrder.getStore().getStoreId(), userId);
        purchaseOrderValidator.requireDraftForSubmit(purchaseOrder.getStatus());

        OffsetDateTime submittedAt = OffsetDateTime.now(ZoneOffset.UTC);
        String orderNo = purchaseOrderNumberGenerator.generate(purchaseOrder.getPurchaseOrderId(), submittedAt);
        purchaseOrder.submit(orderNo, userId, submittedAt);

        return purchaseOrderResponseMapper.toDetailResponse(purchaseOrder);
    }

    @Override
    public PurchaseOrderDetailResponse confirm(Long userId, Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);
        StoreMemberRole role = purchaseOrderValidator.requireManagerOrAbove(purchaseOrder.getStore().getStoreId(), userId);
        purchaseOrderValidator.requireOwner(role);
        purchaseOrderValidator.requireSubmittedForConfirm(purchaseOrder.getStatus());

        purchaseOrder.confirm(userId, OffsetDateTime.now(ZoneOffset.UTC));
        return purchaseOrderResponseMapper.toDetailResponse(purchaseOrder);
    }

    @Override
    public PurchaseOrderDetailResponse cancel(Long userId, Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);
        purchaseOrderValidator.requireManagerOrAbove(purchaseOrder.getStore().getStoreId(), userId);
        purchaseOrderValidator.requireCancelable(purchaseOrder.getStatus());

        purchaseOrder.cancel(userId, OffsetDateTime.now(ZoneOffset.UTC));
        return purchaseOrderResponseMapper.toDetailResponse(purchaseOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadPdf(Long userId, Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);
        purchaseOrderValidator.requireManagerOrAbove(purchaseOrder.getStore().getStoreId(), userId);
        return purchaseOrderPdfService.generate(purchaseOrder);
    }

    private PurchaseOrder getPurchaseOrderOrThrow(Long purchaseOrderId) {
        return purchaseOrderRepository.findWithItemsByPurchaseOrderId(purchaseOrderId)
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
}
