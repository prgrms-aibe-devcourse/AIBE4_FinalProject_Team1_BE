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
/** 발주서의 생성·상태 전이·PDF 다운로드를 처리하는 서비스 구현체다. */
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
    /** 거래처와 품목 정보를 기반으로 발주서 DRAFT를 생성한다. */
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
    /** 특정 매장의 발주서 목록을 최신순으로 조회한다. */
    public List<PurchaseOrderSummaryResponse> getPurchaseOrders(Long userId, Long storeId) {
        purchaseOrderValidator.requireManagerOrAbove(storeId, userId);
        return purchaseOrderRepository.findAllByStoreStoreIdOrderByPurchaseOrderIdDesc(storeId).stream()
                .map(purchaseOrderResponseMapper::toSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    /** 단건 발주서 상세 정보를 조회한다. */
    public PurchaseOrderDetailResponse getPurchaseOrder(Long userId, Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);
        purchaseOrderValidator.requireManagerOrAbove(purchaseOrder.getStore().getStoreId(), userId);
        return purchaseOrderResponseMapper.toDetailResponse(purchaseOrder);
    }

    @Override
    /** DRAFT 상태 발주서의 거래처와 품목을 수정한다. */
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
    /** 발주서를 제출 상태로 전환하고 주문번호를 발급한다. */
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
    /** OWNER 권한으로 제출된 발주서를 확정한다. */
    public PurchaseOrderDetailResponse confirm(Long userId, Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);
        StoreMemberRole role = purchaseOrderValidator.requireManagerOrAbove(purchaseOrder.getStore().getStoreId(), userId);
        purchaseOrderValidator.requireOwner(role);
        purchaseOrderValidator.requireSubmittedForConfirm(purchaseOrder.getStatus());

        purchaseOrder.confirm(userId, OffsetDateTime.now(ZoneOffset.UTC));
        return purchaseOrderResponseMapper.toDetailResponse(purchaseOrder);
    }

    @Override
    /** 취소 가능한 발주서를 취소 상태로 전환한다. */
    public PurchaseOrderDetailResponse cancel(Long userId, Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = getPurchaseOrderOrThrow(purchaseOrderId);
        purchaseOrderValidator.requireManagerOrAbove(purchaseOrder.getStore().getStoreId(), userId);
        purchaseOrderValidator.requireCancelable(purchaseOrder.getStatus());

        purchaseOrder.cancel(userId, OffsetDateTime.now(ZoneOffset.UTC));
        return purchaseOrderResponseMapper.toDetailResponse(purchaseOrder);
    }

    @Override
    @Transactional(readOnly = true)
    /** 발주서 상세를 PDF로 생성해 반환한다. */
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
